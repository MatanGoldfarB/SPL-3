package bgu.spl.net.srv;
import java.io.IOException;
import java.util.HashMap;

public class ConnectionsImpl<T> implements Connections<T>{

    private HashMap<Integer, ConnectionHandler<T>> connections;

    public ConnectionsImpl (){
        this.connections = new HashMap<Integer, ConnectionHandler<T>>();
    }

    public void connect(int connectionId, ConnectionHandler<T> handler){
        this.connections.put(connectionId, handler);
    }

    public boolean send(int connectionId, T msg){
        this.connections.get(connectionId).send(msg);
        return true;
    }

    public void disconnect(int connectionId){
        try {
            this.connections.get(connectionId).close();
        } catch (IOException ignored) {}
    }
}
