package me.glux.mbus.protocal;

public class ResponseEntity {
	private String result;
	private String answerer;
	private String id;
	private String requester;
	private ResponseStatus status;
	public ResponseEntity(){};
	public ResponseEntity(ResponseStatus status,String result){
		this.status=status;
		this.result=result;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getRequester() {
		return requester;
	}
	public void setRequester(String requester) {
		this.requester = requester;
	}
	public String getAnswerer() {
		return answerer;
	}
	public void setAnswerer(String answerer) {
		this.answerer = answerer;
	}

	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public ResponseStatus getStatus() {
		return status;
	}
	public void setStatus(ResponseStatus status) {
		this.status = status;
	}
}
