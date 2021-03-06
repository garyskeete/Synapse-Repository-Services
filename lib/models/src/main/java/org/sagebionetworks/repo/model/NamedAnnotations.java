package org.sagebionetworks.repo.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Separates annotations by name-spaces
 * @author John
 *
 */
public class NamedAnnotations {
	
	private String id; // for its parent entity
	private String etag;
	private Date creationDate;
	private Long createdBy;
	private Map<String, Annotations> map;
	
	public NamedAnnotations(){
		map = new HashMap<String, Annotations>();
		map.put(AnnotationNameSpace.PRIMARY.name(), new Annotations());
		map.put(AnnotationNameSpace.ADDITIONAL.name(), new Annotations());
	}
	
	/**
	 * Get annotations by name.
	 * @param name
	 * @return
	 */
	public Annotations getAnnotationsForName(AnnotationNameSpace name){
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		Annotations annos = map.get(name.name());
		if(annos == null){
			annos = new Annotations();
			map.put(name.name(), annos);
		}
		// Make sure the annotations have the correct metadata
		setMetadate(annos);
		return annos;
	}
	
	/**
	 * 
	 * @param anno
	 */
	private void setMetadate(Annotations anno){
		if(this.id != null){
			anno.setId(this.id);
		}
		if(this.etag != null){
			anno.setEtag(this.etag);
		}
		if(this.creationDate != null){
			anno.setCreationDate(this.creationDate);
		}
	}
	
	public Iterator<AnnotationNameSpace> nameIterator(){
		LinkedList<AnnotationNameSpace> list = new LinkedList<>();
		list.add(AnnotationNameSpace.PRIMARY);
		list.add(AnnotationNameSpace.ADDITIONAL);
		return list.iterator();
	}
	
	/**
	 * Get the primary annotations for this entity.
	 * @return
	 */
	public Annotations getPrimaryAnnotations(){
		return getAnnotationsForName(AnnotationNameSpace.PRIMARY);
	}
	
	/**
	 * Get the additional annotations for this entity.
	 * @return
	 */
	public Annotations getAdditionalAnnotations(){
		return getAnnotationsForName(AnnotationNameSpace.ADDITIONAL);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	
	public void put(AnnotationNameSpace nameSpace, Annotations annos){
		if(nameSpace == null) throw new IllegalArgumentException("Name cannot be null");
		if(annos == null) throw new IllegalArgumentException("Annotations cannot be null");
		map.put(nameSpace.name(), annos);
	}
	
	
	/**
	 * Get a read only copy of the map
	 * @return
	 */
	public Map<String, Annotations> getMap(){
		return Collections.unmodifiableMap(map);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NamedAnnotations other = (NamedAnnotations) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NamedAnnotations [id=" + id + ", etag=" + etag
				+ ", creationDate=" + creationDate + ", map=" + map + "]";
	}

}
