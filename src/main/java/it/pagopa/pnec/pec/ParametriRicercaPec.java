package it.pagopa.pnec.pec;

import javax.validation.constraints.Email;


public class ParametriRicercaPec {
	
	@Email
	protected String email;
	
	protected Integer mailId;
	
	protected Integer userId;
	
	protected String nomeAttach;
	
	protected Double mailboxSpaceLeft;
	
	protected String url;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getMailId() {
		return mailId;
	}

	public void setMailId(Integer mailId) {
		this.mailId = mailId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getNomeAttach() {
		return nomeAttach;
	}

	public void setNomeAttach(String nomeAttach) {
		this.nomeAttach = nomeAttach;
	}

	public Double getMailboxSpaceLeft() {
		return mailboxSpaceLeft;
	}

	public void setMailboxSpaceLeft(Double mailboxSpaceLeft) {
		this.mailboxSpaceLeft = mailboxSpaceLeft;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	

	
	
	
	
}
