package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity(name="ForumThread")
@Table(name = "forum_threads", schema = "jaja@cassandra_pu")
public class ForumThread {

	@Id
	@Column
	private int threadId;

	@Column
	private String title;

    @Column(name = "date")
    private Date date;
    
    @Column
    private String author;

	public int getThreadId() {
		return this.threadId;
	}

	public String getTitle() {
		return this.title;
	}
	
    public Date getDate() {
        return date;
    }
    
    public String getAuthor() {
    	return author;
    }

    public void setDate(Date date) {
        this.date = date;
    }

	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
}
