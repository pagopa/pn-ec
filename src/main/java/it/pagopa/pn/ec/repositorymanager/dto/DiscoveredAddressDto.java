package it.pagopa.pn.ec.repositorymanager.dto;

public class DiscoveredAddressDto {

	protected String name;
	protected String nameRow2;
	protected String address;
	protected String addressRow2;
	protected String cap;
	protected String city;
	protected String city2;	
	protected String pr;
	protected String country;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNameRow2() {
		return nameRow2;
	}
	public void setNameRow2(String nameRow2) {
		this.nameRow2 = nameRow2;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getAddressRow2() {
		return addressRow2;
	}
	public void setAddressRow2(String addressRow2) {
		this.addressRow2 = addressRow2;
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
	public String getCity2() {
		return city2;
	}
	public void setCity2(String city2) {
		this.city2 = city2;
	}
	public String getPr() {
		return pr;
	}
	public void setPr(String pr) {
		this.pr = pr;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	@Override
	public String toString() {
		return "DiscoveredAddressDto [name=" + name + ", nameRow2=" + nameRow2 + ", address=" + address + ", addressRow2="
				+ addressRow2 + ", cap=" + cap + ", city=" + city + ", city2=" + city2 + ", pr=" + pr + ", country="
				+ country + "]";
	}

}
