package model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users", schema = "ForumDB@cassandra_pu")
public class User {

	@Id
	@Column
	private String login;

	@Column
	private String city;

	@Column
	private String dateCreated;

	public User() {
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getLogin() {
		return this.login;
	}

	public String getDateCreated() {
		return this.dateCreated;
	}

	public String getCity() {
		return this.city;
	}

}
