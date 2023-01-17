package it.pagopa.pnec.repositorymanager.model;

public class SenderPhysicalAddress {

	private String name;
	private String address;
	private String cap;
	private String city;
	private String pr;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getCap() {
		return cap;
	}
	public void setCap(String cap) {
		this.cap = cap;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getPr() {
		return pr;
	}
	public void setPr(String pr) {
		this.pr = pr;
	}
	
	@Override
	public String toString() {
		return "SenderPhysicalAddress [name=" + name + ", address=" + address + ", cap=" + cap + ", city=" + city
				+ ", pr=" + pr + "]";
	}
	
}
