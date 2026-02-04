/**
 */
package pnw;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Net</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link pnw.Net#getElements <em>Elements</em>}</li>
 * </ul>
 *
 * @see pnw.PnwPackage#getNet()
 * @model
 * @generated
 */
public interface Net extends NamedElement {
	/**
	 * Returns the value of the '<em><b>Elements</b></em>' containment reference list.
	 * The list contents are of type {@link pnw.NetElement}.
	 * It is bidirectional and its opposite is '{@link pnw.NetElement#getNet <em>Net</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Elements</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Elements</em>' containment reference list.
	 * @see pnw.PnwPackage#getNet_Elements()
	 * @see pnw.NetElement#getNet
	 * @model opposite="net" containment="true" ordered="false"
	 * @generated
	 */
	EList<NetElement> getElements();

} // Net
