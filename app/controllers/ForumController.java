package controllers;

import java.math.BigDecimal;
import java.util.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.avro.generic.GenericData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import model.ForumThread;
import model.Post;
import model.User;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

public class ForumController extends Controller {

	static EntityManagerFactory emf;
	static List<Post> posts = null;
	static Map<String, User> users = null;
	static Map<String, ForumThread> forumThreads = null;
	
	public static Result parseData() throws Exception {
		posts = new ArrayList<Post>();
		users = new HashMap<String, User>();
		forumThreads = new HashMap<String, ForumThread>();
		
		File xmlInput = new File(Play.application().path()+"/public/tolkien.xml");
		
		String parsedTitle = null;
		String parsedCreationDate = null;
		String parsedLocation = null;
		String parsedLogin = null;
		String parsedMessage = null;
		String parsedPostDate = null;
		String parsedPostTitle = null;

		Pattern titlePattern = Pattern.compile("(\\n)Temat: (.*)(\\n\\t{2}\\s{2})");
		Pattern creationDatePattern = Pattern.compile("Dołączył(.{5})(.*)(\\n)");
		Pattern locationPattern = Pattern.compile("Skąd: (.*)(\\n)");
		Pattern generalPattern = Pattern.compile("(\\n)(.*)(\\n\\t{2}\\s{2})", Pattern.DOTALL);	
		Pattern postDatePattern = Pattern.compile("(\\n)Wysłany: (.*)(\\n)(.*)");
		Pattern postTitlePattern = Pattern.compile("(.*)Temat wpisu: (.*)(\\n\\t{2}\\s{2})", Pattern.DOTALL);
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlInput);
		
		NodeList nList = doc.getElementsByTagName("task_result");
		
		for (int temp = 0; temp < nList.getLength(); temp++) {
			 
			Node nNode = nList.item(temp);
			User tmpUser = null;
			ForumThread tmpThread = null;
			Post tmpPost = null;
				
			if(nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				
				//
				// PARSING INFORMATION !!!
				//
				
				// thread title
				String unparsedTitle = eElement.getElementsByTagName("rule").item(0).getTextContent();
				Matcher titleMatcher = titlePattern.matcher(unparsedTitle);
				if(titleMatcher.find())
					parsedTitle = titleMatcher.group(2);
				
				// user creation date
				String unparsedCreationDate = eElement.getElementsByTagName("rule").item(1).getTextContent();
				Matcher creationDateMatcher = creationDatePattern.matcher(unparsedCreationDate);
				if(creationDateMatcher.find()) {
					parsedCreationDate = creationDateMatcher.group(2);
				} else
					parsedCreationDate = "";
				
				// user location
				String unparsedLocation = eElement.getElementsByTagName("rule").item(1).getTextContent();
				Matcher locationMatcher = locationPattern.matcher(unparsedLocation);
				if(locationMatcher.find()) {
					parsedLocation = locationMatcher.group(1);
				} else
					parsedLocation = "";
				
				// user login
				String unparsedLogin = eElement.getElementsByTagName("rule").item(2).getTextContent();
				Matcher loginMatcher = generalPattern.matcher(unparsedLogin);
				if(loginMatcher.find())
					parsedLogin = loginMatcher.group(2);
				
				// message
				String unparsedMessage = eElement.getElementsByTagName("rule").item(3).getTextContent();
				Matcher messageMatcher = generalPattern.matcher(unparsedMessage);
				if(messageMatcher.find())
					parsedMessage = messageMatcher.group(2);
				else
					parsedMessage = "";
				
				// post date
				String unparsedPostDate = eElement.getElementsByTagName("rule").item(4).getTextContent();
				Matcher postDateMatcher = postDatePattern.matcher(unparsedPostDate);
				if(postDateMatcher.find())
					parsedPostDate = postDateMatcher.group(2);
				else
					parsedPostDate = "";
				
				// post title
				String unparsedPostTitle = eElement.getElementsByTagName("rule").item(4).getTextContent();
				Matcher postTitleMatcher = postTitlePattern.matcher(unparsedPostTitle);
				if(postTitleMatcher.find())
					parsedPostTitle = postTitleMatcher.group(2);
				else
					parsedPostTitle = "";
				
				//
				// ADDING ENTRIES TO LISTS / MAPS !!!!
				//
				
				if(users.containsKey(parsedLogin)) {
					tmpUser = users.get(parsedLogin);
				} else {
					tmpUser = new User();
					tmpUser.setLogin(parsedLogin);
					tmpUser.setCity(parsedLocation);
					tmpUser.setDateCreated(parsedCreationDate);
					users.put(parsedLogin, tmpUser);
				}
				
				if(forumThreads.containsKey(parsedTitle)) {
					tmpThread = forumThreads.get(parsedTitle);
				} else {
					tmpThread = new ForumThread();
					tmpThread.setTitle(parsedTitle);
					tmpThread.setThreadId(forumThreads.size()+1);
					forumThreads.put(parsedTitle, tmpThread);
				}
				
				tmpPost = new Post();
				tmpPost.setMessage(parsedMessage);
				tmpPost.setDate(parsedPostDate);
				tmpPost.setTitle(parsedPostTitle);
				tmpPost.setUser(tmpUser);
				tmpPost.setThread(tmpThread);
				tmpPost.setPostId(posts.size()+1);
				posts.add(tmpPost);
			}
		}
		
		return ok("Parsed successfully!");
	}
	
	public static Result persist() {
		
		if(users == null && posts == null && forumThreads == null)
			return ok("Input not parsed yet! Parse input first!");
		
		EntityManager em = getEmf().createEntityManager();
		em.setFlushMode(FlushModeType.COMMIT);
		EntityTransaction tx = em.getTransaction();
		String message = null;
		
		long startTime = System.nanoTime();
		try {
			tx.begin();
			for(User user: users.values()) 
				em.persist(user);
			for(ForumThread thread: forumThreads.values())
				em.persist(thread);
			for(Post post: posts)
				em.persist(post);
			tx.commit();
		} catch (Exception e) {
				try {
					message = "Changes rollbacked - no data persisted.";
					if((tx != null) && (tx.isActive())) {
						tx.rollback();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
		}
		long stopTime = System.nanoTime();

		
		if(message == null) {
			message = "User, thread and post records persisted. Time: "+((stopTime-startTime)/1000000000.0);
		}

		em.close();	
		
		return ok(message);
	}

	public static Result find() {
		EntityManager em = getEmf().createEntityManager();

		User user = em.find(User.class, "Istyar");

		em.close();
		return ok("Found records in database with the following details:" + printUser(user));
	}

    /*
    c
     */
    public static Result avgPostLength() {
        Collection<Post> posts = Post.findAllPosts();

        BigDecimal length = new BigDecimal(0);
        BigDecimal count = new BigDecimal(0);

        for (Post post : posts) {
            length = length.add(new BigDecimal(post.getMessage().length()));
            count = count.add(new BigDecimal(1));
        }

        BigDecimal avg;
        if (!count.equals(new BigDecimal(0))){
            avg = length.divide(count);
        } else {
            avg = new BigDecimal(0);
        }
        return ok("Avg post length: " + avg.toString());
    }

    /*
    f
     */
    public static Result numberOfFrodoPosts() {
        Collection<Post> posts = Post.findAllPosts();

        BigDecimal count = new BigDecimal(0);

        for (Post post : posts) {
            if (post.getMessage().contains("Frodo")) {
                count = count.add(new BigDecimal(1));
            }
        }
        return ok("Number of post with 'Frodo': " + count.toString());
    }

    /*
    g
     */
    public static Result numberOfPostsFromCityK() {
        Collection<Post> posts = Post.findAllPosts();

        BigDecimal count = new BigDecimal(0);

        for (Post post : posts) {
            if (post.getUser().getCity().startsWith("K")) {
                count = count.add(new BigDecimal(1));
            }
        }
        return ok("Number of post from users in 'K' city: " + count.toString());
    }

	public static Result update() {
		EntityManager em = getEmf().createEntityManager();
		em.setFlushMode(FlushModeType.COMMIT);
		EntityTransaction tx = em.getTransaction();
		
		User user = null;
		ForumThread thread = null;
		Post post = null;
		String message = null;
		
		try {
			
			tx.begin();
	
			user = em.find(User.class, "alan");
			user.setCity("New York");
			
			em.merge(user);
			
			thread = em.find(ForumThread.class, 1);
			thread.setTitle("welcome again");
	
			em.merge(thread);
	
			post = em.find(Post.class, 1);
	
			em.merge(post);
			tx.commit();
		
		} catch (Exception e) {
			try {
				message = "Changes rollbacked: ";
				if((tx != null) && (tx.isActive())) {
					tx.rollback();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		em.clear();
		
		user = em.find(User.class, "alan");
		thread = em.find(ForumThread.class, 1);
		post = em.find(Post.class, 1);
		
		if (message == null) {
			message = "Records updated:";
		}

		em.close();
		return ok(message + printUser(user) + printThread(thread) + printPost(post));
	}

	
	public static Result delete() {
		EntityManager em = getEmf().createEntityManager();
		em.setFlushMode(FlushModeType.COMMIT);
		EntityTransaction tx = em.getTransaction();
		
		User user = null;
		ForumThread thread = null;
		Post post = null;
		String message = null;
		
		try {
			tx.begin();
			
			user = em.find(User.class, "alan");
			em.remove(user);
			
			thread = em.find(ForumThread.class, 1);
			em.remove(thread);
			
			post = em.find(Post.class, 1);
			em.remove(post);
			
			tx.commit();
			
		} catch (Exception e) {
			try {
				message = "Changes rollbacked: ";
				if((tx != null) && (tx.isActive())) {
					tx.rollback();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		em.clear();

		user = em.find(User.class, "alan");
		thread = em.find(ForumThread.class, 1);
		post = em.find(Post.class, 1);
		
		if(message == null) {
			message = "Records deleted : ";
		}
		em.close();
		return ok(message + "\nuser: " + printUser(user) + "\nthread: " + printThread(thread)
				+ "\npost: " + printPost(post));
	}

	private static EntityManagerFactory getEmf() {

		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("cassandra_pu");
		}
		return emf;
	}

	private static String printUser(User user) {

		if (user == null) {
			return "\n--------------------------------------------------\nRecord not found";
		}
		return "\n--------------------------------------------------" + "\nlogin:"
				+ user.getLogin() + "\ncity:" + user.getCity() + "\ndate created:"
				+ user.getDateCreated();
	}

	private static String printPost(Post post) {

		if (post == null) {
			return "\n--------------------------------------------------\nRecord not found";
		}
		return "\n--------------------------------------------------" + "\npost:"
				+ post.getPostId() + "\nthread:" + post.getThread().getThreadId() + "\nmessage:"
				+ post.getMessage() + "\ndate:" + post.getDate() + "\nuser:"
				+ post.getUser().getLogin();
	}

	private static String printThread(ForumThread thread) {

		if (thread == null) {
			return "\n--------------------------------------------------\nRecord not found";
		}
		return "\n--------------------------------------------------" + "\nthread id:"
				+ thread.getThreadId() + "\ntitle:" + thread.getTitle();
	}

}
