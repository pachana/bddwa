package controllers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.avro.generic.GenericData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.impetus.client.cassandra.pelops.PelopsClient;
import com.impetus.kundera.client.Client;

import model.ForumThread;
import model.Post;
import model.User;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;
import scala.util.matching.Regex;

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
				Date date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH).parse(parsedPostDate);
				tmpPost.setDate(date);
				tmpPost.setTitle(parsedPostTitle);
				tmpPost.setUser(tmpUser);
				tmpPost.setThread(tmpThread);
				tmpPost.setPostId(posts.size() + 1);
				posts.add(tmpPost);

			}
		}

		return ok("Parsed successfully!");
	}

    public static Post getOldestThreadPost(ForumThread forumThread){
        Post toRet = null;
        for(Post poscik : posts){
            if(poscik.getThread().getTitle() == forumThread.getTitle()) {
                if (toRet != null) {
                    if (poscik.getDate().before(toRet.getDate())) {
                        toRet = poscik;
                    }
                } else {
                    toRet = poscik;
                }
            }
        }
        return toRet;
    }
	
	public static Result persist() {
		
		if(users == null && posts == null && forumThreads == null)
			return ok("Input not parsed yet! Parse input first!");
		
		for(ForumThread ft : forumThreads.values()){
			Post oldest = getOldestThreadPost(ft);
            ft.setDate(oldest.getDate());
            ft.setAuthor(oldest.getUser().getLogin());
        }
		
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
	
    /*
    a
    */
    public static Result numberOfThreadsCreatedIn2013() {
    	long startTime = System.nanoTime();
        List<ForumThread> forumThreads = findAllThreads();
        int size = 0;
    
        for (ForumThread forumThread : forumThreads) {
            if(forumThread.getDate() != null) {
                if (forumThread.getDate().getYear() == 113) {
                    size++;
                }
            }
        }
		long stopTime = System.nanoTime();

        return ok("Number of threads created in 2013: " + size+ " Time: "+((stopTime-startTime)/1000000000.0));
    }

    /*
    b
     */
    public static Result mostPopularThreadMay2013() {
    	long startTime = System.nanoTime();
        List<Post> posts = findAllPosts();
        HashMap<String, BigDecimal> map = new HashMap<String, BigDecimal>();

        for (Post post : posts) {
            if (post.getDate().getMonth() == 4 && post.getDate().getYear() == 113){
                if (map.containsKey(post.getThread().getTitle())) {
                    map.put(post.getThread().getTitle(), map.get(post.getThread().getTitle()).add(new BigDecimal(1)));
                } else {
                    map.put(post.getThread().getTitle(), new BigDecimal(1));
                }
            }
        }
        
        String toReturn = null;
        BigDecimal size = new BigDecimal(0);
        for (Map.Entry<String, BigDecimal> entry : map.entrySet()) {
            if (entry.getValue().compareTo(size) > 0) {
                toReturn = entry.getKey();
                size = entry.getValue();
            }
        }
		long stopTime = System.nanoTime();

        if (toReturn != null) {
            return ok("Most popular thread in may 2013: " + toReturn + " with: " + size.toString() + " posts."+ " Time: "+((stopTime-startTime)/1000000000.0));
        } else {
            return ok("Something went wrong :(");
        }
    }
    
    /*
    c
     */
    public static Result avgPostLength() {
    	long startTime = System.nanoTime();
    	List<Post> posts = findAllPosts();
        BigDecimal length = new BigDecimal(0);
        BigDecimal count = new BigDecimal(0);

        for (Post post : posts) {
            length = length.add(new BigDecimal(post.getMessage().length()));
            count = count.add(new BigDecimal(1));
        }

        BigDecimal avg;
        if (!count.equals(new BigDecimal(0))){
            avg = length.divide(count, MathContext.DECIMAL128);
        } else {
            avg = new BigDecimal(0);
        }
		long stopTime = System.nanoTime();

        return ok("Avg post length: " + avg.toString()+ ". Time: "+((stopTime-startTime)/1000000000.0));
    }

    /*
    d
     */
    public static Result mostThreadsUser() {
    	long startTime = System.nanoTime();
        List<Post> posts = findAllPosts();

        HashMap<String, List<String>> map = new HashMap<String, List<String>>();

        for (Post post : posts) {
            if (map.containsKey(post.getUser().getLogin())) {
                if (!map.get(post.getUser().getLogin()).contains(post.getThread().getTitle())) {
                    map.get(post.getUser().getLogin()).add(post.getThread().getTitle());
                }
            } else {
                ArrayList<String> tmp = new ArrayList<String>();
                tmp.add(post.getThread().getTitle());
                map.put(post.getUser().getLogin(), tmp);
            }
        }

        String toReturn = null;
        int size = 0;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getValue().size() > size) {
                toReturn = entry.getKey();
                size = entry.getValue().size();
            }
        }
		long stopTime = System.nanoTime();

        if (toReturn != null) {
            return ok("User with most threads: " + toReturn + " with " + size + " threads."+ " Time: "+((stopTime-startTime)/1000000000.0));
        } else {
            return ok("Something went wrong :(");
        }
    }
    
    /*
     e
     */
    public static Result mostCommentedUser() {
    	long startTime = System.nanoTime();
    	List<Post> allPosts = findAllPosts();
    	
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
        
        for (Post post : allPosts) {
            if(post.getUser().getLogin() != post.getThread().getAuthor()) {
                if (map.containsKey(post.getUser().getLogin())) {
                    map.get(post.getUser().getLogin()).add(post.getThread().getAuthor());
                } else {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(post.getThread().getAuthor());
                    map.put(post.getUser().getLogin(), tmp);
                }
            }
        }

        String toReturn = null;
        int size = 0;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getValue().size() > size) {
                toReturn = entry.getKey();
                size = entry.getValue().size();
            }
        }
		long stopTime = System.nanoTime();

        if (toReturn != null) {
            return ok("User commented most other user: " + toReturn + " with " + size + " comments."+ " Time: "+((stopTime-startTime)/1000000000.0));
        } else {
            return ok("Something went wrong :(");
        }
    }

    /*
    f
     */
    public static Result numberOfFrodoPosts() {
    	long startTime = System.nanoTime();
        List<Post> posts = findAllPosts();

        BigDecimal count = new BigDecimal(0);

        for (Post post : posts) {
            if (post.getMessage().contains("Frodo")) {
                count = count.add(new BigDecimal(1));
            }
        }
		long stopTime = System.nanoTime();

        return ok("Number of post with 'Frodo': " + count.toString()+ ". Time: "+((stopTime-startTime)/1000000000.0));
    }

    /*
    g
     */
    public static Result numberOfPostsFromCityK() {
    	long startTime = System.nanoTime();
        List<Post> posts = findAllPosts();

        BigDecimal count = new BigDecimal(0);

        for (Post post : posts) {
            if (post.getUser().getCity().startsWith("K")) {
                count = count.add(new BigDecimal(1));
            }
        }
		long stopTime = System.nanoTime();

        return ok("Number of post from users in 'K' city: " + count.toString()+ ". Time: "+((stopTime-startTime)/1000000000.0));
    }

    /*
    h
     */
    public static Result word35MostUsed() {
    	long startTime = System.nanoTime();
        List<Post> posts = findAllPosts();

        HashMap<String, BigDecimal> map = new HashMap<String, BigDecimal>();

        for (Post post : posts) {
            String[] wordTab = post.getMessage().split(" ");
            for (int i = 0; i < wordTab.length; i++) {
                if (map.containsKey(wordTab[i])) {
                	map.put(wordTab[i], map.get(wordTab[i]).add(new BigDecimal(1)));
                } else {
                    map.put(wordTab[i], new BigDecimal(1));
                }
            }
        }

        ValueComparator bvc =  new ValueComparator(map);
        TreeMap<String,BigDecimal> sorted_map = new TreeMap<String,BigDecimal>(bvc);
        sorted_map.putAll(map);
        
        String word = sorted_map.keySet().toArray()[34].toString();
		long stopTime = System.nanoTime();
        
        return ok("35. most used word: " + word + " with " + map.get(word) + " appearances." + " Time: "+((stopTime-startTime)/1000000000.0));
    }

    static class ValueComparator implements Comparator<String> {

        Map<String, BigDecimal> base;
        public ValueComparator(Map<String, BigDecimal> base) {
            this.base = base;
        }

        public int compare(String a, String b) {
            if(base.get(a).compareTo(base.get(b)) >= 0)
                return -1;
            else
                return 1;
        }
    }
    
    public static List<Post> findAllPosts() {
    	List<Post> posts = new ArrayList<Post>();
    	EntityManager em = getEmf().createEntityManager();
		int i=1;
		boolean flag = true;
    	while(flag) {
    		Post post = em.find(Post.class, i);
    		i++;
    		if(post == null) 
    			flag = false;
    		else
    			posts.add(post);
    	}
	    em.close();
	    return posts;
    }

    public static List<ForumThread> findAllThreads(){
        List<ForumThread> forumThreads = new ArrayList<ForumThread>();
        EntityManager em = getEmf().createEntityManager();
        int i=1;
        boolean flag = true;
        while(flag) {
            ForumThread forumThread = em.find(ForumThread.class, i);
            i++;
            if(forumThread == null)
                flag = false;
            else
                forumThreads.add(forumThread);
        }
        em.close();
        return forumThreads;
    }

	private static EntityManagerFactory getEmf() {

		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("cassandra_pu");
		}
		return emf;
	}

}
