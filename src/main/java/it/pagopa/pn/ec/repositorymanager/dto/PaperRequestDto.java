package it.pagopa.pn.ec.repositorymanager.dto;

import java.util.List;
import java.util.Map;

public class PaperRequestDto {

	private String iun;
	private String requestPaid;
	private String productType;
	private List<PaperEngageRequestAttachmentsDto> attachments;
	private String printType;
	private String receiverName;
	private String receiverNameRow2;
	private String receiverAddress;
	private String receiverAddressRow2;
	private String receiverCap;
	private String receiverCity;
	private String receiverCity2;
	private String receiverPr;
	private String receiverCountry;
	private String receiverFiscalCode;
	private String senderName;
	private String senderAddress;
	private String senderCity;
	private String senderPr;
	private String senderDigitalAddress;
	private String arName;
	private String arAddress;
	private String arCap;
	private String arCity;
	private Map<String, String> vas;
	
	public String getIun() {
		return iun;
	}
	public void setIun(String iun) {
		this.iun = iun;
	}
	public String getRequestPaid() {
		return requestPaid;
	}
	public void setRequestPaid(String requestPaid) {
		this.requestPaid = requestPaid;
	}
	public String getProductType() {
		return productType;
	}
	public void setProductType(String productType) {
		this.productType = productType;
	}
	public List<PaperEngageRequestAttachmentsDto> getAttachments() {
		return attachments;
	}
	public void setAttachments(List<PaperEngageRequestAttachmentsDto> attachments) {
		this.attachments = attachments;
	}
	public String getPrintType() {
		return printType;
	}
	public void setPrintType(String printType) {
		this.printType = printType;
	}
	public String getReceiverName() {
		return receiverName;
	}
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
	public String getReceiverNameRow2() {
		return receiverNameRow2;
	}
	public void setReceiverNameRow2(String receiverNameRow2) {
		this.receiverNameRow2 = receiverNameRow2;
	}
	public String getReceiverAddress() {
		return receiverAddress;
	}
	public void setReceiverAddress(String receiverAddress) {
		this.receiverAddress = receiverAddress;
	}
	public String getReceiverAddressRow2() {
		return receiverAddressRow2;
	}
	public void setReceiverAddressRow2(String receiverAddressRow2) {
		this.receiverAddressRow2 = receiverAddressRow2;
	}
	public String getReceiverCap() {
		return receiverCap;
	}
	public void setReceiverCap(String receiverCap) {
		this.receiverCap = receiverCap;
	}
	public String getReceiverCity() {
		return receiverCity;
	}
	public void setReceiverCity(String receiverCity) {
		this.receiverCity = receiverCity;
	}
	public String getReceiverCity2() {
		return receiverCity2;
	}
	public void setReceiverCity2(String receiverCity2) {
		this.receiverCity2 = receiverCity2;
	}
	public String getReceiverPr() {
		return receiverPr;
	}
	public void setReceiverPr(String receiverPr) {
		this.receiverPr = receiverPr;
	}
	public String getReceiverCountry() {
		return receiverCountry;
	}
	public void setReceiverCountry(String receiverCountry) {
		this.receiverCountry = receiverCountry;
	}
	public String getReceiverFiscalCode() {
		return receiverFiscalCode;
	}
	public void setReceiverFiscalCode(String receiverFiscalCode) {
		this.receiverFiscalCode = receiverFiscalCode;
	}
	public String getSenderName() {
		return senderName;
	}
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	public String getSenderAddress() {
		return senderAddress;
	}
	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	public String getSenderCity() {
		return senderCity;
	}
	public void setSenderCity(String senderCity) {
		this.senderCity = senderCity;
	}
	public String getSenderPr() {
		return senderPr;
	}
	public void setSenderPr(String senderPr) {
		this.senderPr = senderPr;
	}
	public String getSenderDigitalAddress() {
		return senderDigitalAddress;
	}
	public void setSenderDigitalAddress(String senderDigitalAddress) {
		this.senderDigitalAddress = senderDigitalAddress;
	}
	public String getArName() {
		return arName;
	}
	public void setArName(String arName) {
		this.arName = arName;
	}
	public String getArAddress() {
		return arAddress;
	}
	public void setArAddress(String arAddress) {
		this.arAddress = arAddress;
	}
	public String getArCap() {
		return arCap;
	}
	public void setArCap(String arCap) {
		this.arCap = arCap;
	}
	public String getArCity() {
		return arCity;
	}
	public void setArCity(String arCity) {
		this.arCity = arCity;
	}
	public Map<String, String> getVas() {
		return vas;
	}
	public void setVas(Map<String, String> vas) {
		this.vas = vas;
	}
	
	@Override
	public String toString() {
		return "PaperRequestDto [iun=" + iun + ", requestPaid=" + requestPaid + ", productType=" + productType
				+ ", attachments=" + attachments + ", printType=" + printType + ", receiverName=" + receiverName
				+ ", receiverNameRow2=" + receiverNameRow2 + ", receiverAddress=" + receiverAddress
				+ ", receiverAddressRow2=" + receiverAddressRow2 + ", receiverCap=" + receiverCap + ", receiverCity="
				+ receiverCity + ", receiverCity2=" + receiverCity2 + ", receiverPr=" + receiverPr
				+ ", receiverCountry=" + receiverCountry + ", receiverFiscalCode=" + receiverFiscalCode
				+ ", senderName=" + senderName + ", senderAddress=" + senderAddress + ", senderCity=" + senderCity
				+ ", senderPr=" + senderPr + ", senderDigitalAddress=" + senderDigitalAddress + ", arName=" + arName
				+ ", arAddress=" + arAddress + ", arCap=" + arCap + ", arCity=" + arCity + ", vas=" + vas + "]";
	}
	
}
