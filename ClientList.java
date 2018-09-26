import java.util.HashSet;
import java.util.Set;

/**
 * This handles the clientList for the program.
 */
public class ClientList {
    Set<Integer> clientList;

    public ClientList() {
        clientList = new HashSet<Integer>();
    }

    /**
     * @param clientList the clientList to set
     */
    public void setClientList(Set<Integer> clientList) {
        this.clientList = clientList;
    }

    public void add(int id) {
        this.clientList.add(id);
    }

    public void remove(int id) {
        this.clientList.remove(id);
    }

    public void printList() {
        for (Integer e : clientList) {
            System.out.println("List number: " + e);
        }
    }

    /**
     * @return the clientList
     */
    public Set<Integer> getClientList() {
        return clientList;
    }
}