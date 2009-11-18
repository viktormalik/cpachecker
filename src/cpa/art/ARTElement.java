package cpa.art;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

public class ARTElement implements AbstractElementWithLocation, AbstractWrapperElement{

  private final ARTCPA mCpa;
  private final AbstractElementWithLocation element;
  private final Set<ARTElement> children;
  public static long artElementEqualsTime = 0; 
  private final Set<ARTElement> parents; // more than one parent if joining elements
  private ARTElement mCoveredBy = null;

  private int elementId;
  private static int nextArtElementId= 0;

  private int mark;

  protected ARTElement(ARTCPA pCpa, AbstractElementWithLocation pAbstractElement, ARTElement pParentElement) {
    mCpa = pCpa;
    element = pAbstractElement;
    parents = new HashSet<ARTElement>();
    if(pParentElement != null){
      addParent(pParentElement);
    }
    children = new HashSet<ARTElement>();
    elementId = ++nextArtElementId;
    mark = 0;
  }

  public Set<ARTElement> getParents(){
    return parents;
  }

  protected void addParent(ARTElement pOtherParent){
    if(parents.add(pOtherParent)){
      pOtherParent.children.add(this);
    }
  }

  public Set<ARTElement> getChildren(){
    return children;
  }

  public AbstractElementWithLocation getAbstractElementOnArtNode(){
    return element;
  }

  public boolean isMarked() { 
    return mark > 0; 
  }

  protected void setMark() { 
    mark = nextArtElementId++; 
  }

  protected void setMark(int pMark) { 
    mark = pMark; 
  }

  public int getMark() { 
    return mark; 
  }

  protected void setCovered(ARTElement pCoveredBy) {
    assert pCoveredBy != null;
    mCoveredBy = pCoveredBy;
    mCpa.setCovered(this, true);
  }
  
  protected void setUncovered() {
    mCoveredBy = null;
    mCpa.setCovered(this, false);
  }
  
  public boolean isCovered() {
    return mCpa.isCovered(this);
  }
  
  public ARTElement getCoveredBy() {
    return mCoveredBy;
  }
  
  public ARTCPA getCpa() {
    return mCpa;
  }
  
  @Override
  public String toString() {
    String s = "\n";
    s = s + "ART Element Id: " + elementId + " : Mark: "+ mark +", ";
//    if(parentElement != null){
//      s = s + "Parent Element's Id: " + getParent().elementId + ", ";
//      for(ARTElement additionalParent: additionalParents){
//        s = s + "Add. Parent's Id: " + additionalParent.elementId + ", ";
//      }
//    }
//    else{
//      s = s + "parent is null" + ", ";
//    }
    s = s + " CHILDS > " + children.size() + ", ";
    s = s + element;
    return s;
  }

  @Override
  public CFANode getLocationNode() {
    return element.getLocationNode();
  }

  @Override
  public AbstractElement retrieveElementOfType(String pElementClass) {
    if(element.getClass().getSimpleName().equals(pElementClass)){
      return element;
    }
    else{
      return ((AbstractWrapperElement)element).retrieveElementOfType(pElementClass);
    }
  }

  // TODO check
  public Set<ARTElement> getSubtree() {
    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.add(this);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (result.add(currentElement)) {
        // currentElement was not in result
        workList.addAll(currentElement.children);
      }
    }
    return result;
  }

  /**
   * This method removes all children of this element from the ART.
   * 
   * Note that it does not remove any elements from the covered set, this has to
   * be done by the caller.
   */
  protected void clearChildren() {
    for (ARTElement child : children) {
      assert (child.parents.contains(this));
      child.parents.remove(this);
    }
    children.clear();
  }


  /**
   * This method removes this element from the ART by removing it from its
   * parents' children list and from its children's parents list.
   * 
   * This method also removes the element from the set of covered elements.
   * 
   * This means, if its children do not have any other parents, they will be not
   * reachable any more, i.e. they do not belong to the ART any more. But those
   * elements will not be removed from the covered set.
   */
  protected void removeFromART() {
    clearChildren();
    
    // clearParents
    for (ARTElement parent : parents) {
      assert (parent.children.contains(this));
      parent.children.remove(this);
    }
    parents.clear();

    setUncovered();
  }
    
  public int getElementId() {
    return elementId;
  }

  @Override
  public boolean isError() {
    return element.isError();
  }

  /**
   * This method returns a random element from the list of parents.
   * @return A parent of this element.
   */
  public ARTElement getFirstParent() {
    if (parents.isEmpty()) {
      return null;
    }
    Iterator<ARTElement> it = parents.iterator();
    return it.next();
  }
}
