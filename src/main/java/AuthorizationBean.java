import cloud.Dropbox;
import commons.Tokens;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import persistence.HibernateUtil;
import persistence.UserEntity;
import persistence.UserManager;

import javax.ejb.Stateless;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 7/5/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */

@Stateless
public class AuthorizationBean implements AuthorizationBeanRemote {

    @Override
    public Long login(String login, String password) {
        UserManager userManager = new UserManager();
        Query query = userManager.getSession().createQuery("from UserEntity where login=:login and password=:password");
        query.setString("login", login);
        query.setString("password", password);
        List<UserEntity> list = query.list();
        if(list == null || list.size() < 1){
            return null;
        }
        userManager.finalize();
        return list.get(0).getId();
    }

    @Override
    public Boolean registerUser(String login, String password) {
        UserManager userManager = null;
        try{
            userManager = new UserManager();
            UserEntity newUser = new UserEntity();
            newUser.setLogin(login);
            newUser.setPassword(password);
            userManager.addUser(newUser);
        }catch (NullPointerException e){
            e.printStackTrace();
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            if(userManager != null){
                userManager.finalize();
            }
        }
        return true;
    }

    @Override
    public String getDropboxAuthLink(Long userId) {
        String link = null;
        Boolean res = false;

        Dropbox drop = new Dropbox();

        Tokens requestTokens = drop.getRequestTokens();

        // save requestTokens to DB
        UserManager manager = new UserManager();
        UserEntity user = manager.getUserById(userId);
        user.setDropboxRequestKey(requestTokens.key);
        user.setDropboxRequestSecret(requestTokens.secret);
        res = manager.updateUser(user);
        manager.finalize();
        if ( res == false ){
            //  error
        }

        link = drop.getAuthLink();

        return link;
    }

    @Override
    public Boolean retrieveDropboxAccessToken(Long userId) {
        boolean res = false;

        // Work with dropbox service, start session
        Dropbox drop = new Dropbox();

        // get requestTokens from db
        UserManager manager = new UserManager();
        UserEntity user = manager.getUserById(userId);
        Tokens requestTokens = new Tokens(user.getDropboxRequestKey(), user.getDropboxRequestSecret());

        // retrive AccessToken
        Tokens accessTokens = drop.getUserAccessTokens(requestTokens);

        // save accessTokens to DB
        user.setDropboxAccessKey(accessTokens.key);
        user.setDropboxAccessSecret(accessTokens.secret);
        res = manager.updateUser(user);
        manager.finalize();

        return res;
    }
}