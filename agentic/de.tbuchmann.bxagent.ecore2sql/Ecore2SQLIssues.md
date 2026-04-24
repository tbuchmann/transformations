# Ecore2SQL Incremental Backward — Issue Report

## Failing test: `IncrementalBackward.testIncrementalInserts`

### Symptom

After `createDataElementTable()` is applied to the SQL and the backward incremental transformation runs:

| | Feature |
|---|---|
| **Expected** | `data : DataElement [0..1]` — EReference on DataNode |
| **Actual** | `data : EInt [0..1]` — EAttribute on DataNode |

---

## SQL state analysis

### After `createDataNodeTable()` (step 2)

The `data` column in the DataNode table is:

```java
.column().name("data").type("int")
    .annotation().name("single").end(ColumnBuilder.class)
    .annotation().name("attribute").end(ColumnBuilder.class)
```

- Annotations: `"attribute"` + `"single"`
- **No FK to any table**

### After `createDataElementTable()` (step 5)

This helper:
1. Creates the DataElement table (annotated `"class"` + `"abstract"`)
2. Adds `"DataElement"` column (FK to DataElement) to the **EObject** table

It does **not** touch the DataNode table's `data` column.
After step 5 the DataNode.`data` column still has annotations `"attribute"` + `"single"` and still has **no FK to DataElement**.

---

## How our backward transformation works

Phase 1f (backward CTM incremental) dispatches columns by annotation:

| Backward condition | Creates |
|---|---|
| `_hasAnnotation(col, "attribute") && _hasAnnotation(col, "single")` | `EAttribute` (type from SQL column type: `"int"` → EInt) |
| `_hasAnnotation(col, "cross") && _hasAnnotation(col, "single")` | `EReference` |

Because DataNode.`data` still has `"attribute"` + `"single"`, the backward CTM correctly creates `data : EInt [0..1]` — which is **right given the SQL state it can observe**.

No other backward rule (CROSS_REF TLM, INHERITANCE, BIDIRECTIONAL_CROSS_REF, CONTAINMENT_SINGLE/MULTI) applies to this column, because there is no FK and no matching annotation set.

---

## Root cause

There is a **semantic mismatch** between the SQL state produced by the helpers and the SQL state our transformation expects in order to produce the correct Ecore output.

For our transformation to produce `data : DataElement [0..1]` as an EReference, the DataNode.`data` column must carry:
- annotations `"cross"` + `"single"` + `"unidirectional"` (instead of `"attribute"` + `"single"`), AND
- a FK from `DataNode.data` to the `DataElement` table

Neither of these is established by `createDataNodeTable()` (step 2) or `createDataElementTable()` (step 5).

---

## Why BXtend solved it

BXtend likely employs a **hippocratic / consistency-restoring** backward strategy: it starts from the previous Ecore model and modifies it minimally to satisfy the new SQL state. This means:

- If the previous Ecore had `data : DataElement` (either from a prior forward run or from the initial model),  
  BXtend *preserves* it as long as the SQL does not explicitly contradict it.
- Our transformation is **pure-backward**: it derives Ecore solely from the annotations and FK structure present in the SQL. It has no memory of what `data` "used to mean".

This is a fundamental difference in transformation philosophy, not a bug in our implementation per se.

---

## Open questions (need clarification)

### Q1 — Does one of the other helpers update DataNode.`data`?

`createDataElementTable()`, `createPairTable()`, `createValueTable()`, and `createKeyTable()` are all applied together in step 5. We have only seen `createDataNodeTable()` and `createDataElementTable()`. If `createPairTable()`, `createValueTable()`, or `createKeyTable()` **also modifies the DataNode.`data` column** (adds FK to DataElement, changes annotations to `"cross"` + `"single"`), then **the helper is fine and our transformation is also fine** — the SQL state it sees would be correct.

**Action**: Please provide the bodies of `createPairTable()`, `createValueTable()`, and `createKeyTable()` to confirm or rule this out.

### Q2 — What is the "CompositeListSimpleSQL" postcondition model?

If the reference SQL model `"CompositeListSimpleDataSQL"` has DataNode.`data` annotated `"cross"` + `"single"` with a FK to DataElement, this confirms that the SQL helper is incomplete. If it still shows `"attribute"` + `"single"`, it confirms that a hippocratic backward strategy is expected.

### Q3 — What does the "CompositeListSimpleDataEcore" model contain for DataNode?

If DataNode in the reference Ecore model has `data : DataElement [0..1]` as an EReference, and the SQL helper does not supply the cross-ref structure, then either:
- The helper needs to be extended (SQL-helper fix), or
- Our transformation needs a hippocratic element (preserve source features not contradicted by target).

---

## Potential fixes

### Fix A — Update the SQL helper (correct if Q2 shows the SQL should have "cross"+"single")

`createDataElementTable()` should additionally:
1. Find DataNode.`data` column
2. Remove `"attribute"` annotation, add `"cross"` + `"unidirectional"`
3. Add FK from DataNode.`data` → DataElement table (annotated `"single"` + `"unidirectional"` + `"cross"`)

### Fix B — Add a backward CROSS_REF resolution step (correct if Q2 shows the SQL stays as-is)

In `resolveReferencesIncrementalBack`, add a section that:
1. Iterates all FK links in the SQL that are not annotated (no `"containment"`, `"superType"`, `"bidirectional"`)
2. For each such FK: resolves owner EClass and target EClass via `corrIndex.inverse()`
3. If the owner EClass already has an **EAttribute** with that column's name whose type doesn't match → deletes it, creates EReference
4. Also updates the column's annotations to `"cross"` + `"single"` so that the CTM handles it correctly in subsequent incremental runs

### Fix C — Hippocratic backward (correct if BXtend's approach is intentional)

After the backward CTM and TLM resolution, add a reconciliation pass that:
1. Compares the previous Ecore model with the newly produced one
2. For each feature that changed type (e.g. EAttribute → nothing, while the EClass it was typed to now exists), preserves the feature as EReference if the SQL does not explicitly contradict it

---

## Recommendation

Await answers to Q1 before implementing anything. If one of the other helpers establishes the FK on DataNode.`data`, **the transformation is correct and only the helper is missing the connection**. If none of them does, Fix A (helper update) is the cleanest solution because it keeps the transformation logic annotation-driven and SQL-authoritative.
