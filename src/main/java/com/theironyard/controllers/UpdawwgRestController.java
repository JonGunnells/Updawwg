package com.theironyard.controllers;

import com.theironyard.entities.Dog;
import com.theironyard.entities.Post;
import com.theironyard.entities.User;
import com.theironyard.services.DogRepository;
import com.theironyard.services.PostRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utils.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by will on 7/7/16.
 */
@RestController
public class UpdawwgRestController {
    // link tables
    @Autowired
    DogRepository dogs;

    @Autowired
    UserRepository users;

    @Autowired
    PostRepository posts;

    // start h2 web server
    @PostConstruct
    public void init() throws SQLException {
        Server.createWebServer().start();
    }

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String home(HttpSession session, Model model, String name, String image, String breed, int age, String description, Boolean favorite, String search) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "dawgIn";

        } else {
            User user = users.findFirstByName(username);
            Iterable<Dog> doggies;
            if (search != null) {
                doggies = dogs.searchDog(search);
            } else if (name != null) {
                doggies = dogs.findByName(name);
            } else if (breed != null) {
                doggies = dogs.findByBreed(breed);
            }
            else {
                doggies = dogs.findByUser(user);
            }

            model.addAttribute("dogs", doggies);
            return "feed";
        }
    }

    // get/post routes for users
    @RequestMapping(path = "/users", method = RequestMethod.GET)
    public Iterable<User> getUsers() {
        return users.findAll();
    }

    // make login happen in here
    @RequestMapping(path = "/users", method = RequestMethod.POST)
    public void user(User user, HttpSession session) throws Exception {
        User userFromDB = users.findFirstByName(user.getName());
        if (userFromDB == null) {
            user.setPassword(PasswordStorage.createHash(user.getPassword()));
            users.save(user);
        }
        else if (!PasswordStorage.verifyPassword(user.getPassword(), userFromDB.getPassword())) {
            throw new Exception("Wrong password!");
        }

        session.setAttribute("username", user.getName());
    }

    // routes for dogs
    @RequestMapping(path = "/dogs", method = RequestMethod.GET)
    public Iterable<Dog> getDogs() {
        return dogs.findAll();
    }

    @RequestMapping(path = "/dogs", method = RequestMethod.POST)
    public void dog(HttpSession session,String name, String breed, int age, String description, Boolean favorite, MultipartFile photo) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in!");
        }

        File dir = new File("public/assets");
        dir.mkdirs();
        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), dir);
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Dog dog = new Dog(name, photoFile.getName(), breed, age, description, favorite);


        dogs.save(dog);
    }

    // routes for posts
    @RequestMapping(path = "/posts", method = RequestMethod.GET)
    public Iterable<Post> getPosts() {
        return posts.findAll();
    }

    @RequestMapping(path = "/posts", method = RequestMethod.POST)
    public void post(HttpSession session, int replyId, String message, int dogId, int userId) throws Exception {

        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in!");
        }

        Dog dog = dogs.findOne(dogId);
        User user = users.findOne(userId);
        Post post = new Post(replyId, message, user, dog);
        posts.save(post);
    }
}
