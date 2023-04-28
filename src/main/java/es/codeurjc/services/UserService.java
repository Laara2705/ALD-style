package es.codeurjc.services;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service that manage all about authenticate and serves the process of login
 * and register of someone inside the Web Application
 * @author gu4re
 * @version 1.9
 */
@Service
public class UserService implements Serializable {
	/**
	 * Fake database that controls the users registered in the App
	 * and maps the email of the User (Key) with the password previously hashed (Value)
	 */
	private static Map<String, String> usersMap;
	
	/**
	 * Private constructor avoiding initialize of Service
	 */
	private UserService(){}
	
	/**
	 * Starts the UserService connecting the database to the Service.<br><br>
	 * An <a style="color: #ff6666; display: inline;"><b>error</b></a> can occur
	 * <a style="color: #E89B6C; display: inline;">if the database is not found</a>
	 * (IOException) <a style="color: #E89B6C; display: inline;">or</a> reading
	 * <a style="color: #E89B6C; display: inline;">casting fails</a> during deserialize
	 * process inside database (ClassNotFoundException) in
	 * <a style="color: #E89B6C; display: inline;">both cases</a> the app will run with
	 * a <a style="color: #E89B6C; display: inline;">new and empty database</a>
	 * @deprecated <a style="color: #E89B6C; display: inline;">
	 * This method can <b>only</b> be used by SpringBeanSystem</a> so abstain from using it
	 */
	@EventListener
	@SuppressWarnings(value = "unchecked, unused")
	@Deprecated(since = "1.3")
    public static void run(ContextRefreshedEvent event) {
		try (ObjectInputStream ois = new ObjectInputStream(
		        new FileInputStream("src/main/resources/database/database.bin"))) {
		    Object obj = ois.readObject();
	        usersMap = (Map<String, String>) obj;
		} catch (IOException e) {
			Logger.getLogger("Unable to find database or maybe does not exists.");
		    usersMap = new HashMap<>();
		} catch (ClassNotFoundException e){
			Logger.getLogger("Unable to read content of database.");
			usersMap = new HashMap<>();
		}
	}
	
	/**
	 * Stops the UserService before the Spring Application ends, saving the database
	 * to a binary file
	 * @deprecated <a style="color: #E89B6C; display: inline;">
	 * This method can <b>only</b> be used by SpringBeanSystem</a> so abstain from using it
	 */
	@SuppressWarnings(value = "unused")
	@Deprecated(since = "1.2")
	@EventListener
	public static void stop(ContextClosedEvent event){
		try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("src/main/resources/database/database.bin"))) {
            oos.writeObject(usersMap);
			usersMap.clear();
        } catch (IOException e) {
            Logger.getLogger("Unable to reach file to write on.");
        }
	}
	
	/**
	 * Carriers the authenticate process of the web application,
	 * checking if the user passed as parameter is already registered
	 * @param email The email to be checked
	 * @param rawPassword The plain password to be checked
	 * @return <a style="color: #E89B6C; display: inline;">True</a> if the user is already
	 * registered otherwise <a style="color: #E89B6C; display: inline;">False</a>
	 */
	public static boolean authenticate(@NotNull String email, @NotNull String rawPassword){
		try{
			return usersMap.get(email) != null
		       && usersMap.get(email).equals(SecurityService.hashCode(rawPassword));
		} catch (NoSuchAlgorithmException e){
			Logger.getLogger("Failed hash function.");
		}
		return false;
	}
	
	/**
	 * Check if a user exists or not
	 * @param email The user's email to be checked
	 * @return <a style="color: #E89B6C; display: inline;">True</a> if the user exists
	 * otherwise <a style="color: #E89B6C; display: inline;">False</a>
	 */
	public static boolean userExists(@NotNull String email){
		return usersMap.get(email) != null;
	}
	
	/**
	 * Register the user inside the database
	 * @param rawPassword The user's plain password to be added into database
	 * @param email The user's email to be added into database
	 */
	public static void addUser(@NotNull String email, @NotNull String rawPassword){
		try {
			usersMap.put(email, SecurityService.hashCode(rawPassword));
		} catch (NoSuchAlgorithmException e){
			Logger.getLogger("Failed hash function.");
			addUser(email, rawPassword);
		}
	}
	
	/**
	 * Changes the password of the user that has email passed as parameter
	 * @param email the user's mail
	 * @return A ResponseEntity based of what happened in the Service
	 * returning <a style="color: #E89B6C; display: inline;">200 OK</a> if all goes good,
	 * <a style="color: #E89B6C; display: inline;">400 Bad Request</a> if not and
	 * otherwise <a style="color: #E89B6C; display: inline;">404 Not Found</a> if any resource is not able
	 */
	public static @NotNull ResponseEntity<Void> changePassword(@NotNull String email, @NotNull String newRawPassword){
		try{
			if (usersMap.put(email, SecurityService.hashCode(newRawPassword)) == null)
				throw new NullPointerException();
			return ResponseEntity.ok().build();
		} catch (NoSuchAlgorithmException e){
			Logger.getLogger("Failed hash function.");
			return ResponseEntity.notFound().build();
		} catch (NullPointerException e){
			Logger.getLogger("No mapping found for that credentials.");
			return ResponseEntity.badRequest().build();
		}
	}
	
	/**
	 * Remove a user passed an email
	 * @param email the email passed by the controller
	 */
	public static void removeUser(@NotNull String email){
		usersMap.remove(email);
	}
}