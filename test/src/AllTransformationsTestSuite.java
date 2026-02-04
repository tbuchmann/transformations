import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  Bags22Bags1Test.class,  
  Bags12Bags2Test.class,
  Oset2SetTest.class,
  Set2OsetTest.class,
  Pdb22Pdb1Test.class,
  Pdb12Pdb2Test.class,
  Pnw2PnTest.class,
  Pn2PnwTest.class,
  Cpm2GanttTest.class,
  Gantt2CpmTest.class,
  Dag2AstTest.class,
  Ast2DagTest.class,
  Sql2EcoreTest.class,
  Ecore2SqlTest.class,
})

public class AllTransformationsTestSuite {
}
