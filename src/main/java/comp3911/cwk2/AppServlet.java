package comp3911.cwk2;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {

  private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";
  private static final String AUTH_QUERY = "select * from user where username=? and password=?";
  private static final String SEARCH_QUERY = "select * from patient where surname=? collate nocase";

  private final Configuration fm = new Configuration(Configuration.VERSION_2_3_28);
  private Connection database;

  @Override
  public void init() throws ServletException {
    configureTemplateEngine();
    connectToDatabase();
  }

  private void configureTemplateEngine() throws ServletException {
    try {
      fm.setDirectoryForTemplateLoading(new File("./templates"));
      fm.setDefaultEncoding("UTF-8");
      fm.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
      fm.setLogTemplateExceptions(false);
      fm.setWrapUncheckedExceptions(true);
    }
    catch (IOException error) {
      throw new ServletException(error.getMessage());
    }
  }

  private void connectToDatabase() throws ServletException {
    try {
      database = DriverManager.getConnection(CONNECTION_URL);
    }
    catch (SQLException error) {
      throw new ServletException(error.getMessage());
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {
    try {
      Template template = fm.getTemplate("login.html");
      template.process(null, response.getWriter());
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (TemplateException error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
   throws ServletException, IOException {
     // Get form parameters
    //String username = request.getParameter("username");
    //String password = request.getParameter("password");
    //String surname = request.getParameter("surname");

    try {
      if (authenticated(request.getParameter("username"), request.getParameter("password"))) {
        // Get search results and merge with template
        Map<String, Object> model = new HashMap<>();
        model.put("records", searchResults(request.getParameter("surname")));
        Template template = fm.getTemplate("details.html");
        template.process(model, response.getWriter());
      }
      else {
        Template template = fm.getTemplate("invalid.html");
        template.process(null, response.getWriter());
      }
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (Exception error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  // Updated function which prevents sql injection
  private boolean authenticated(String username, String password) throws SQLException, NoSuchAlgorithmException {
    // get hashed password
    String hashPassword = createHashPassword(password);
    // setting up prepared statement
    try (PreparedStatement stmt = database.prepareStatement(AUTH_QUERY)) {
      // setting parameters
      stmt.setString(1, username);
      stmt.setString(2, hashPassword);
      // execute query
      ResultSet results = stmt.executeQuery();
      
      return results.next();
    }catch (SQLException e) {
      e.printStackTrace(); // Handle the exception appropriately in your application
      return false; // Or some other error handling mechanism
    }
  }

  // updated function that checks if surname has a ; indicating a sql injection
  private List<Record> searchResults(String surname) throws SQLException {
    List<Record> records = new ArrayList<>();
    // if surname contains a ; we suspect a sql injection
    if(!surname.contains(String.valueOf(";"))){
      // setting up prepared statement
      try (PreparedStatement stmt = database.prepareStatement(SEARCH_QUERY)) {
        // setting parameters
        stmt.setString(1,surname);
        // execute query
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
          Record rec = new Record();
          rec.setSurname(results.getString(2));
          rec.setForename(results.getString(3));
          rec.setAddress(results.getString(4));
          rec.setDateOfBirth(results.getString(5));
          rec.setDoctorId(results.getString(6));
          rec.setDiagnosis(results.getString(7));
          records.add(rec);
        }
      }
    }
    
    return records;
  }

  //function to get all passwords from db and hash them
  private void hashPasswords() throws NoSuchAlgorithmException{
    // SQL commands
    String query = "SELECT password FROM user;";
    String updateQuery = "UPDATE user SET password = '%s' WHERE password = '%s';";

    // get all passwords from the db
    try (Statement stmt = database.createStatement()){
      ResultSet passwords = stmt.executeQuery(query);
      int i = 0;
      while (passwords.next()) {  
        // hash password
        String password = passwords.getString("password");        
        String hashPassword = createHashPassword(password);
        // update password in the db  
        String updatedQuery = String.format(updateQuery,hashPassword,password);
        PreparedStatement preparedstmt = database.prepareStatement(updatedQuery);
        int check = preparedstmt.executeUpdate();
      }
      
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // Function to take a password and hash it
  private String createHashPassword(String password) throws NoSuchAlgorithmException{

    // create instance of the SHA-256 algorthim
    MessageDigest hash = MessageDigest.getInstance("SHA-256");
    // hash the password 
    byte[] byteHashPassword = hash.digest(password.getBytes());
    
    // loop through byte array
    String hashPassword = "";
    int length = byteHashPassword.length, i;
    for(i=0;i<length;i++){
      // convert to hex and append to string
      hashPassword+=(String.format("%02x", byteHashPassword[i]));
    }

    //System.out.println(hashPassword);
    return hashPassword;
  }
}


