package com.firebase.security.token;

import java.util.Date;

/**
 * Token options.
 *
 * @author vikrum
 */
public class TokenOptions {

    private Date expires;
    private Date notBefore;
    private boolean admin;
    private boolean debug;

    /**
     * Copies the date, since Date objects are mutable.
     */
    private Date copyDate(Date date) {
        return (date != null) ? new Date(date.getTime()) : null;
    }

    /**
     * Default constructor.
     */
    public TokenOptions() {
        expires = null;
        notBefore = null;
        admin = false;
        debug = false;
    }

    /**
     * Parametrized constructor.
     *
     * @param expires   The date/time at which the token should no longer be considered valid. (default is never).
     * @param notBefore The date/time before which the token should not be considered valid. (default is now).
     * @param admin     Set to true to bypass all security rules (you can use this for trusted server code).
     * @param debug     Set to true to enable debug mode (so you can see the results of Rules API operations).
     */
    public TokenOptions(Date expires, Date notBefore, boolean admin, boolean debug) {
        super();
        this.expires = copyDate(expires);
        this.notBefore = copyDate(notBefore);
        this.admin = admin;
        this.debug = debug;
    }

    /**
     * @return the expires
     */
    public Date getExpires() {
        return expires;
    }

    /**
     * @param expires the expires to set
     */
    public void setExpires(Date expires) {
        this.expires = copyDate(expires);
    }

    /**
     * @return the notBefore
     */
    public Date getNotBefore() {
        return notBefore;
    }

    /**
     * @param notBefore the notBefore to set
     */
    public void setNotBefore(Date notBefore) {
        this.notBefore = copyDate(notBefore);
    }

    /**
     * @return the admin
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @param admin the admin to set
     */
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
