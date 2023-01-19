package it.pagopa.pn.ec.repositorymanager.model;

public class GeneratedMessage {

	private String system;
	private String id;
	private String location;
	
	public String getSystem() {
		return system;
	}
	public void setSystem(String system) {
		this.system = system;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	@Override
	public String toString() {
		return "GeneratedMessage [system=" + system + ", id=" + id + ", location=" + location + "]";
	}

}
