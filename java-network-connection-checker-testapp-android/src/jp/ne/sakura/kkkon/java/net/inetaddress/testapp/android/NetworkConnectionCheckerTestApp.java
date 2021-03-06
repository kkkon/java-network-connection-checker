/*
 * The MIT License
 * 
 * Copyright (C) 2014 Kiyofumi Kondoh
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
package jp.ne.sakura.kkkon.java.net.inetaddress.testapp.android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import jp.ne.sakura.kkkon.java.net.inetaddress.NetworkConnectionChecker;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;

/**
 *
 * @author Kiyofumi Kondoh
 */
public class NetworkConnectionCheckerTestApp extends Activity
{
    public static final String TAG = "appKK";

    private InetAddress destHost = null;
    private boolean isReachable = false;

    private TextView textView = null;
    private Handler handler = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        final Context context = this.getApplicationContext();

        {
            NetworkConnectionChecker.initialize();
        }

        super.onCreate(savedInstanceState);

        /* Create a TextView and set its content.
         * the text is retrieved by calling a native
         * function.
         */
        LinearLayout layout = new LinearLayout( this );
        layout.setOrientation( LinearLayout.VERTICAL );

        TextView  tv = new TextView(this);
        tv.setText( "reachable=" );
        layout.addView( tv );
        this.textView = tv;

        Button btn1 = new Button( this );
        btn1.setText( "invoke Exception" );
        btn1.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                final int count = 2;
                int[] array = new int[count];
                int value = array[count]; // invoke IndexOutOfBOundsException
            }
        } );
        layout.addView( btn1 );

        {
            Button btn = new Button( this );
            btn.setText( "disp isReachable" );
            btn.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {
                    final boolean isReachable = NetworkConnectionChecker.isReachable();
                    Toast toast = Toast.makeText( context, "IsReachable=" + isReachable , Toast.LENGTH_LONG );
                    toast.show();
                }
            } );
            layout.addView( btn );
        }

        {
            Button btn = new Button( this );
            btn.setText( "upload http AsyncTask" );
            btn.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {
                    AsyncTask<String,Void,Boolean>   asyncTask = new AsyncTask<String,Void,Boolean>() {

                        @Override
                        protected Boolean doInBackground(String... paramss) {
                            Boolean result = true;
                            Log.d( TAG, "upload AsyncTask tid=" + android.os.Process.myTid() );
                            try
                            {
                                //$(BRAND)/$(PRODUCT)/$(DEVICE)/$(BOARD):$(VERSION.RELEASE)/$(ID)/$(VERSION.INCREMENTAL):$(TYPE)/$(TAGS)
                                Log.d( TAG, "fng=" + Build.FINGERPRINT );
                                final List<NameValuePair> list = new ArrayList<NameValuePair>(16);
                                list.add( new BasicNameValuePair( "fng", Build.FINGERPRINT ) );

                                HttpPost    httpPost = new HttpPost( paramss[0] );
                                //httpPost.getParams().setParameter( CoreConnectionPNames.SO_TIMEOUT, new Integer(5*1000) );
                                httpPost.setEntity( new UrlEncodedFormEntity( list, HTTP.UTF_8 ) );
                                DefaultHttpClient   httpClient = new DefaultHttpClient();
                                Log.d( TAG, "socket.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.SO_TIMEOUT, -1) );
                                Log.d( TAG, "connection.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, -1) );
                                httpClient.getParams().setParameter( CoreConnectionPNames.SO_TIMEOUT, new Integer(5*1000) );
                                httpClient.getParams().setParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(5*1000) );
                                Log.d( TAG, "socket.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.SO_TIMEOUT, -1) );
                                Log.d( TAG, "connection.timeout=" + httpClient.getParams().getIntParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, -1) );
                                // <uses-permission android:name="android.permission.INTERNET"/>
                                // got android.os.NetworkOnMainThreadException, run at UI Main Thread
                                HttpResponse response = httpClient.execute( httpPost );
                                Log.d( TAG, "response=" + response.getStatusLine().getStatusCode() );
                            }
                            catch ( Exception e )
                            {
                                Log.d( TAG, "got Exception. msg=" + e.getMessage(), e );
                                result = false;
                            }
                            Log.d( TAG, "upload finish" );
                            return result;
                        }


                    };

                    asyncTask.execute("http://kkkon.sakura.ne.jp/android/bug");
                    asyncTask.isCancelled();
                }
            } );
            layout.addView( btn );
        }

        {
            Button btn = new Button( this );
            btn.setText( "pre DNS query(0.0.0.0)" );
            btn.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {
                    isReachable = false;
                    Thread thread = new Thread( new Runnable() {

                        public void run() {
                            try
                            {
                                destHost = InetAddress.getByName("0.0.0.0");
                                if ( null != destHost )
                                {
                                    try
                                    {
                                        if ( destHost.isReachable( 5*1000 ) )
                                        {
                                            Log.d( TAG, "destHost=" + destHost.toString() + " reachable" );
                                        }
                                        else
                                        {
                                            Log.d( TAG, "destHost=" + destHost.toString() + " not reachable" );
                                        }
                                    }
                                    catch ( IOException e )
                                    {
                                        
                                    }
                                }
                            }
                            catch ( UnknownHostException e )
                            {

                            }
                            Log.d( TAG, "destHost=" + destHost );
                        }
                    });
                    thread.start();
                    try
                    {
                        thread.join( 1000 );
                    }
                    catch ( InterruptedException e )
                    {
                        
                    }
                }
            });
            layout.addView( btn );
        }
        {
            Button btn = new Button( this );
            btn.setText( "pre DNS query(www.google.com)" );
            btn.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {
                    isReachable = false;
                    Thread thread = new Thread( new Runnable() {

                        public void run() {
                            Log.d( TAG, "start" );
                            try
                            {
                                InetAddress dest = InetAddress.getByName("www.google.com");
                                if ( null == dest )
                                {
                                    dest = destHost;
                                }

                                if ( null != dest )
                                {
                                    final String[] uris = new String[] { "http://www.google.com/", "https://www.google.com/" };
                                    for ( final String destURI : uris )
                                    {
                                        URI uri = null;
                                        try
                                        {
                                            uri = new URI( destURI );
                                        }
                                        catch ( URISyntaxException e )
                                        {
                                            //Log.d( TAG, e.toString() );
                                        }

                                        if ( null != uri )
                                        {
                                            URL url = null;
                                            try
                                            {
                                                url = uri.toURL();
                                            }
                                            catch (MalformedURLException ex)
                                            {
                                                Log.d( TAG, "got exception:" + ex.toString(), ex );
                                            }

                                            URLConnection conn = null;
                                            if ( null != url )
                                            {
                                                Log.d( TAG, "openConnection before" );
                                                try
                                                {
                                                    conn = url.openConnection();
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
                                                Log.d( TAG, "openConnection after" );
                                                if ( conn instanceof HttpURLConnection )
                                                {
                                                    HttpURLConnection httpConn = (HttpURLConnection)conn;
                                                    int responceCode = -1;
                                                    try
                                                    {
                                                        Log.d( TAG, "getResponceCode before" );
                                                        responceCode = httpConn.getResponseCode();
                                                        Log.d( TAG, "getResponceCode after" );
                                                    }
                                                    catch (IOException ex)
                                                    {
                                                        Log.d( TAG, "got exception:" + ex.toString(), ex );
                                                    }
                                                    Log.d( TAG, "responceCode=" + responceCode );
                                                    if ( 0 < responceCode )
                                                    {
                                                        isReachable = true;
                                                        destHost = dest;
                                                    }
                                                    Log.d( TAG, " HTTP ContentLength=" + httpConn.getContentLength() );
                                                    httpConn.disconnect();
                                                    Log.d( TAG, " HTTP ContentLength=" + httpConn.getContentLength() );
                                                }
                                            }
                                        } // if uri

                                        if ( isReachable )
                                        {
                                            //break;
                                        }
                                    } // for uris
                                }
                                else
                                {
                                }
                            }
                            catch ( UnknownHostException e )
                            {
                                Log.d( TAG, "dns error" + e.toString() );
                                destHost = null;
                            }
                            {
                                if ( null != destHost )
                                {
                                    Log.d( TAG, "destHost=" + destHost );
                                }
                            }
                            Log.d( TAG, "end" );
                        }
                    });
                    thread.start();
                    try
                    {
                        thread.join();
                        {
                            final String addr = (null==destHost)?(""):(destHost.toString());
                            final String reachable = (isReachable)?("reachable"):("not reachable");
                            Toast toast = Toast.makeText( context, "DNS result=\n" + addr + "\n " + reachable, Toast.LENGTH_LONG );
                            toast.show();
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        
                    }
                }
            });
            layout.addView( btn );
        }

        {
            Button btn = new Button( this );
            btn.setText( "pre DNS query(kkkon.sakura.ne.jp)" );
            btn.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {
                    isReachable = false;
                    Thread thread = new Thread( new Runnable() {

                        public void run() {
                            Log.d( TAG, "start" );
                            try
                            {
                                InetAddress dest = InetAddress.getByName("kkkon.sakura.ne.jp");
                                if ( null == dest )
                                {
                                    dest = destHost;
                                }
                                if ( null != dest )
                                {
                                    try
                                    {
                                        if ( dest.isReachable( 5*1000 ) )
                                        {
                                            Log.d( TAG, "destHost=" + dest.toString() + " reachable" );
                                            isReachable = true;
                                        }
                                        else
                                        {
                                            Log.d( TAG, "destHost=" + dest.toString() + " not reachable" );
                                        }
                                        destHost = dest;
                                    }
                                    catch ( IOException e )
                                    {
                                        
                                    }
                                }
                                else
                                {
                                }
                            }
                            catch ( UnknownHostException e )
                            {
                                Log.d( TAG, "dns error" + e.toString() );
                                destHost = null;
                            }
                            {
                                if ( null != destHost )
                                {
                                    Log.d( TAG, "destHost=" + destHost );
                                }
                            }
                            Log.d( TAG, "end" );
                        }
                    });
                    thread.start();
                    try
                    {
                        thread.join();
                        {
                            final String addr = (null==destHost)?(""):(destHost.toString());
                            final String reachable = (isReachable)?("reachable"):("not reachable");
                            Toast toast = Toast.makeText( context, "DNS result=\n" + addr + "\n " + reachable, Toast.LENGTH_LONG );
                            toast.show();
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        
                    }
                }
            });
            layout.addView( btn );
        }

        {
            Button btn = new Button( this );
            btn.setText( "pre DNS query(kkkon.sakura.ne.jp) support proxy" );
            btn.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View view)
                {
                    isReachable = false;
                    Thread thread = new Thread( new Runnable() {

                        public void run() {
                            try
                            {
                                String  target = null;
                                {
                                    ProxySelector proxySelector = ProxySelector.getDefault();
                                    Log.d( TAG, "proxySelector=" + proxySelector );
                                    if ( null != proxySelector )
                                    {
                                        URI uri = null;
                                        try
                                        {
                                            uri = new URI("http://www.google.com/");
                                        }
                                        catch ( URISyntaxException e )
                                        {
                                            Log.d( TAG, e.toString() );
                                        }
                                        List<Proxy> proxies = proxySelector.select( uri );
                                        if ( null != proxies )
                                        {
                                            for ( final Proxy proxy : proxies )
                                            {
                                                Log.d( TAG, " proxy=" + proxy );
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
                                if ( null == target )
                                {
                                    target = "kkkon.sakura.ne.jp";
                                }
                                InetAddress dest = InetAddress.getByName(target);
                                if ( null == dest )
                                {
                                    dest = destHost;
                                }
                                if ( null != dest )
                                {
                                    try
                                    {
                                        if ( dest.isReachable( 5*1000 ) )
                                        {
                                            Log.d( TAG, "destHost=" + dest.toString() + " reachable" );
                                            isReachable = true;
                                        }
                                        else
                                        {
                                            Log.d( TAG, "destHost=" + dest.toString() + " not reachable" );
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
                                                                                Log.d( TAG, "got Exception" + e.toString(), e );
                                                                            }
                                                                            if ( conn instanceof HttpURLConnection )
                                                                            {
                                                                                HttpURLConnection httpConn = (HttpURLConnection)conn;
                                                                                if ( 0 < httpConn.getResponseCode() )
                                                                                {
                                                                                    isReachable = true;
                                                                                }
                                                                                Log.d( TAG, " HTTP ContentLength=" + httpConn.getContentLength() );
                                                                                Log.d( TAG, " HTTP res=" + httpConn.getResponseCode() );
                                                                                //httpConn.setInstanceFollowRedirects( false );
                                                                                //httpConn.setRequestMethod( "HEAD" );
                                                                                //conn.connect();
                                                                                httpConn.disconnect();
                                                                                Log.d( TAG, " HTTP ContentLength=" + httpConn.getContentLength() );
                                                                                Log.d( TAG, " HTTP res=" + httpConn.getResponseCode() );
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
                                        destHost = dest;
                                    }
                                    catch ( IOException e )
                                    {
                                        Log.d( TAG, "got Excpetion " + e.toString() );
                                    }
                                }
                                else
                                {
                                }
                            }
                            catch ( UnknownHostException e )
                            {
                                Log.d( TAG, "dns error" + e.toString() );
                                destHost = null;
                            }
                            {
                                if ( null != destHost )
                                {
                                    Log.d( TAG, "destHost=" + destHost );
                                }
                            }
                        }
                    });
                    thread.start();
                    try
                    {
                        thread.join();
                        {
                            final String addr = (null==destHost)?(""):(destHost.toString());
                            final String reachable = (isReachable)?("reachable"):("not reachable");
                            Toast toast = Toast.makeText( context, "DNS result=\n" + addr + "\n " + reachable, Toast.LENGTH_LONG );
                            toast.show();
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        
                    }
                }
            });
            layout.addView( btn );
        }

        setContentView( layout );
    }

    @Override
    protected void onStart() {
        NetworkConnectionChecker.start( false );

        this.handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                //Log.d( TAG, "hnadleMessage" );
                final boolean isReachable = NetworkConnectionChecker.isReachable();
                final long lastTime = NetworkConnectionChecker.getLastCheckedTime();
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis( lastTime );
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                textView.setText( "reachable=" + isReachable + "\n" + "time=" + sdf.format( cal.getTime() ) );
                //textView.invalidate();

                removeMessages(0);
                sendMessageDelayed( obtainMessage(0), 10*1000 );
            }

        };
        this.handler.sendMessage( this.handler.obtainMessage(0) );

        super.onStart(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void onStop() {
        NetworkConnectionChecker.stop();

        this.handler = null;

        super.onStop(); //To change body of generated methods, choose Tools | Templates.
    }


}
