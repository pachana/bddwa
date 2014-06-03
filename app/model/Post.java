package model;

import javax.persistence.*;

import java.util.Collection;
import java.util.List;

@Entity(name="Post")
@Table(name = "posts", schema = "jaja@cassandra_pu")
public class Post {

	@Id
	@Column
	private int postId;

	@ManyToOne
	@JoinColumn(name = "thread")
	private ForumThread thread;
	
	@Column
	private String message;

	@Column
	private String date;
	
	@Column
	private String title;

	@ManyToOne
	@JoinColumn(name = "user")
	private User user;

	public void setPostId(int postId) {
		this.postId = postId;
	}

	public void setThread(ForumThread thread2) {
		this.thread = thread2;
		//thread.getPosts().add(this);
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setUser(User user) {
		this.user = user;
		//user.getPosts().add(this);
	}

	public int getPostId() {
		return this.postId;
	}

	public ForumThread getThread() {
		return this.thread;
	}

	public String getMessage() {
		return this.message;
	}
	
	public String getTitle() {
		return this.title;
	}

	public String getDate() {
		return this.date;
	}

	public User getUser() {
		return this.user;
	}
}
