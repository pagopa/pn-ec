package it.pagopa.pnec.repositorymanager.dto;

public class GeneratedMessageDto {

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
		return "GeneratedMessageDto [system=" + system + ", id=" + id + ", location=" + location + "]";
	}

}
