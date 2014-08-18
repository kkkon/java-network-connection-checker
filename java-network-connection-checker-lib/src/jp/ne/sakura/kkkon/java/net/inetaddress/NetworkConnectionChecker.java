/*
 * The MIT License
 *
 * Copyright 2014 Kiyofumi Kondoh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ne.sakura.kkkon.java.net.inetaddress;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class NetworkConnectionChecker
{
    private static boolean isAndroid = false;

    /*
     * 'www.google.com' is low ttl.( 300 second )
     * But this server drop ECHO TCP SYN.
     * Not responce 'Connection Refuse'. blackhole it...
     */
    private static final String hostnameLowTTL = "www.google.com";

    private static String hostname = "kkkon.sakura.ne.jp";
    private static volatile boolean isReachable = false;

    private static ResolverThread thread = null;

    public static synchronized void initialize( final String host )
    {
        Class<?>    clazz = null;
        try
        {
            clazz = Class.forName( "android.os.Build" );
            isAndroid = true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
        }

        if ( null != host )
        {
            hostname = host;
        }
    }

    public static boolean isReachable()
    {
        return isReachable;
    }


    public static synchronized  void start()
    {
        if ( null == thread )
        {
            thread = new ResolverThread();
            thread.setName( "DNS Resolver KK" );
            thread.start();
        }
    }

    public static synchronized  void stop()
    {
        if ( null != thread )
        {
            if ( thread.isAlive() )
            {
                thread.interrupt();
            }

            try
            {
                thread.join();
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
            }

            thread = null;
        }
    }

    private static class ResolverThread extends Thread
    {

        @Override
        public void run()
        {
            isReachable = false;

            while( !this.isInterrupted() )
            {
                try
                {
                    Thread.sleep( 15 * 1000 );
                }
                catch (InterruptedException ex)
                {
                    Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
                isReachable = false;

                String  target = null;
                {
                    ProxySelector proxySelector = ProxySelector.getDefault();
                    //Log.d( TAG, "proxySelector=" + proxySelector );
                    if ( null != proxySelector )
                    {
                        URI uri = null;
                        try
                        {
                            uri = new URI("http://www.google.com/");
                        }
                        catch ( URISyntaxException e )
                        {
                            //Log.d( TAG, e.toString() );
                        }

                        if ( null != uri )
                        {
                            List<Proxy> proxies = proxySelector.select( uri );
                            if ( null != proxies )
                            {
                                for ( final Proxy proxy : proxies )
                                {
                                    //Log.d( TAG, " proxy=" + proxy );
                                    if ( null != proxy )
                                    {
                                        if ( Proxy.Type.HTTP == proxy.type() )
                                        {
                                            final SocketAddress sa = proxy.address();
                                            if ( sa instanceof InetSocketAddress )
                                            {
                                                final InetSocketAddress isa = (InetSocketAddress)sa;
                                                target = isa.getHostName();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if ( null == target )
                {
                    target = hostnameLowTTL;
                    InetAddress dest = null;
                    try
                    {
                        dest = InetAddress.getByName( target );
                    }
                    catch (UnknownHostException ex)
                    {
                        Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if ( null == dest )
                    {
                        continue;
                    }
                    else
                    {
                        target = hostname;
                        try
                        {
                            dest = InetAddress.getByName( target );
                        }
                        catch (UnknownHostException ex)
                        {
                            Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if ( null == dest )
                        {
                            continue;
                        }
                        try
                        {
                            if ( dest.isReachable( 5*1000 ) )
                            {
                                //Log.d( TAG, "destHost=" + dest.toString() + " reachable" );
                                isReachable = true;
                            }
                            else
                            {
                                //Log.d( TAG, "destHost=" + dest.toString() + " not reachable" );
                            }
                        }
                        catch ( IOException ex )
                        {
                            Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                else
                {
                    InetAddress dest = null;
                    try
                    {
                        dest = InetAddress.getByName( target );
                    }
                    catch (UnknownHostException ex)
                    {
                        Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if ( null == dest )
                    {
                        continue;
                    }
                    try
                    {
                        if ( dest.isReachable( 5*1000 ) )
                        {
                            //Log.d( TAG, "destHost=" + dest.toString() + " reachable" );
                            isReachable = true;
                        }
                        else
                        {
                            //Log.d( TAG, "destHost=" + dest.toString() + " not reachable" );
                            {
                                ProxySelector proxySelector = ProxySelector.getDefault();
                                //Log.d( TAG, "proxySelector=" + proxySelector );
                                if ( null != proxySelector )
                                {
                                    URI uri = null;
                                    try
                                    {
                                        uri = new URI("http://www.google.com/");
                                    }
                                    catch ( URISyntaxException e )
                                    {
                                        //Log.d( TAG, e.toString() );
                                    }

                                    if ( null != uri )
                                    {
                                        List<Proxy> proxies = proxySelector.select( uri );
                                        if ( null != proxies )
                                        {
                                            for ( final Proxy proxy : proxies )
                                            {
                                                //Log.d( TAG, " proxy=" + proxy );
                                                if ( null != proxy )
                                                {
                                                    if ( Proxy.Type.HTTP == proxy.type() )
                                                    {
                                                        URL url = uri.toURL();
                                                        URLConnection conn = null;
                                                        if ( null != url )
                                                        {
                                                            try
                                                            {
                                                                conn = url.openConnection( proxy );
                                                                if ( null != conn )
                                                                {
                                                                    conn.setConnectTimeout( 3*1000 );
                                                                    conn.setReadTimeout( 3*1000 );
                                                                }
                                                            }
                                                            catch ( IOException e )
                                                            {
                                                                //Log.d( TAG, "got Exception" + e.toString(), e );
                                                            }
                                                            if ( conn instanceof HttpURLConnection )
                                                            {
                                                                HttpURLConnection httpConn = (HttpURLConnection)conn;
                                                                if ( 0 < httpConn.getResponseCode() )
                                                                {
                                                                    isReachable = true;
                                                                }
                                                                //Log.d( TAG, " HTTP ContentLength=" + httpConn.getContentLength() );
                                                                //Log.d( TAG, " HTTP res=" + httpConn.getResponseCode() );
                                                                httpConn.disconnect();
                                                                //Log.d( TAG, " HTTP ContentLength=" + httpConn.getContentLength() );
                                                                //Log.d( TAG, " HTTP res=" + httpConn.getResponseCode() );
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                    catch ( IOException ex )
                    {
                        Logger.getLogger(NetworkConnectionChecker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } // while
        }
    }
}
