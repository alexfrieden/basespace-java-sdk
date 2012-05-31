/**
* Copyright 2012 Illumina
* 
 * Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*    http://www.apache.org/licenses/LICENSE-2.0
* 
 *  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package com.illumina.basespace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author bking
 *
 */
class HttpServer implements Runnable
{
    private static Logger logger = Logger.getLogger(HttpServer.class.getPackage().getName());
    private List<AuthCodeListener>authListeners =  Collections.synchronizedList(new ArrayList<AuthCodeListener>());
    
    private boolean running = true;
    private int port = 7777;
    private int timeoutSeconds = 30;
    
    HttpServer(int listenerPort,int timeoutSeconds)
    {
        this.port = listenerPort;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public void run()
    {
        Socket socket = null;
        BufferedWriter writer = null;
        BufferedReader reader = null;
        
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(500);
            
            logger.info("SDK internal server listening for Auth Code request at " 
                    +  serverSocket.getInetAddress().toString() + ":" + serverSocket.getLocalPort());

            String authCode = null;
            long timeout = System.currentTimeMillis() + (timeoutSeconds*1000);
            boolean timedOut = false;
            while (running)
            {
                try
                {
                    socket = serverSocket.accept();
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String m = reader.readLine();
                    if (m != null)
                    {
                        authCode = getAuthCode(m);

                        String responseText = authCode != null?
                                "Authorization has been sent to the SDK. You can close the browser."
                                :"There was an error receiving the auth code";
                        
                        writer.write("HTTP/1.0 200 OK");
                        writer.newLine();
                        writer.write("Content-Type: text/html");
                        writer.newLine();
                        writer.newLine();
                        writer.write("<html><body><pre>");
                        writer.newLine();
                        writer.write(responseText);
                        writer.newLine();
                  
                        while ((m = reader.readLine()) != null)
                        {
                           if (m.length() == 0) break;
                           writer.newLine();
                        }
                        writer.write("</pre></body></html>");
                        writer.newLine();
                        writer.flush();
                    }
                    if (authCode != null)
                    {
                        running = false;
                    }
                }
                catch(SocketTimeoutException timeoutEx)
                {
                    timedOut = System.currentTimeMillis() > timeout;
                    if (timedOut)
                    {
                        running = false;
                        fireTimedOutEvent();
                    }
                }
                catch(Throwable t)
                {
                    t.printStackTrace();
                }
                finally
                {
                    if (writer !=  null)
                    {
                        try{writer.close();}catch(Throwable t){}
                    }
                    if (reader !=  null)
                    {
                        try{reader.close();}catch(Throwable t){}
                    }
                    if (socket !=  null)
                    {
                        try{socket.close();}catch(Throwable t){}
                    } 
                }
            }//While server running
            if (authCode != null)
            {
                fireAuthCodeEvent(new AuthCodeEvent(this,authCode));
            }
            logger.info("Exiting server thread");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            removeAllAuthCodeListeners();
        }
    }
    
    private static String getAuthCode(String params)
    {
        final String START = "GET /?";
        final String END = " HTTP/1.1";
        if ( params.indexOf(START) == -1 ||  params.indexOf(END) == -1)return null;
        params = params.substring(START.length(),params.length()-END.length());
        Map<String, String>map =  getQueryMap(params);
        return map.get("code");
    }
   
    private  static Map<String, String> getQueryMap(String query)   
    {   
        String[] params = query.split("&");   
        Map<String, String> map = new HashMap<String, String>();   
        for (String param : params)   
        {   
            String name = param.split("=")[0];   
            String value = param.split("=")[1];   
            map.put(name, value);   
        }   
        return map;   
    } 
    
    boolean isRunning()
    {
        return running;
    }
    void setRunning(boolean running)
    {
        this.running = running;
    }

    void addAuthCodeListener(AuthCodeListener listener)
    {
        if (!authListeners.contains(listener))authListeners.add(listener);
    }
    void removeAuthCodeListener(AuthCodeListener listener)
    {
        if (authListeners.contains(listener))authListeners.remove(listener);
    }
    void removeAllAuthCodeListeners()
    {
        while(authListeners.size() > 0)
        {
            authListeners.remove(0);
        }
    }
    protected void fireAuthCodeEvent(AuthCodeEvent evt)
    {
        for(AuthCodeListener listener:authListeners)
        {
            listener.authCodeReceived(evt);
        }
    }
    protected void fireTimedOutEvent()
    {
        EventObject obj = new EventObject(this);
        for(AuthCodeListener listener:authListeners)
        {
            listener.timedOut(obj);
        }
    }
}