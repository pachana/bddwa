package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "forum_threads", schema = "ForumDB@cassandra_pu")
public class ForumThread {

	@Id
	@Column
	private int threadId;

	@Column
	private String title;

	public int getThreadId() {
		return this.threadId;
	}

	public String getTitle() {
		return this.title;
	}

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
