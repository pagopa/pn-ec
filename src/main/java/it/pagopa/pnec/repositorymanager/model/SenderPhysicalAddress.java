package it.pagopa.pnec.repositorymanager.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

@DynamoDBDocument
public class SenderPhysicalAddress {

	private String name;
	private String address;
	private String cap;
	private String city;
	private String pr;

	@DynamoDBAttribute(attributeName = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@DynamoDBAttribute(attributeName = "address")
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	@DynamoDBAttribute(attributeName = "cap")
	public String getCap() {
		return cap;
	}
	public void setCap(String cap) {
		this.cap = cap;
	}
	@DynamoDBAttribute(attributeName = "city")
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	@DynamoDBAttribute(attributeName = "pr")
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
