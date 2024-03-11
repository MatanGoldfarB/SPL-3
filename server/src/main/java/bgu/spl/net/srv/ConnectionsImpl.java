package bgu.spl.net.srv;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class ConnectionsImpl<T> implements Connections<T>{

    public ConcurrentHashMap<Integer, BlockingConnectionHandler<T>> connections;

    public ConnectionsImpl (){
        this.connections = new ConcurrentHashMap<Integer, BlockingConnectionHandler<T>>();
    }

    public void connect(int connectionId, ConnectionHandler<T> handler){
        this.connections.put(connectionId, (BlockingConnectionHandler<T>)handler);
    }

    public synchronized boolean send(int connectionId, T msg){
        this.connections.get(connectionId).send(msg);
        return true;
    }

    public void disconnect(int connectionId){
        try {
            this.connections.get(connectionId).close();
            this.connections.remove(connectionId);
        } catch (IOException ignored) {}
    }

    public BlockingConnectionHandler<T> getHandler(int id){
        return this.connections.get(id);
    }

    public boolean userNameExist(String userName){
        for (BlockingConnectionHandler<T> handler : connections.values()) {
            if(handler.getUserName().equals(userName)){
                return true;
            }
        }
        return false;
    }

    public Set<Integer> getKeySet(){
        return connections.keySet();
    }
}
