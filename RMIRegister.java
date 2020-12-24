import java.rmi.Remote;
import java.rmi.RemoteException;

//interfaccia per implementare la registrazione dell'utente tramite RMI 
public interface RMIRegister extends Remote{
	
	public String registra_utente(String nickUtente, String password) throws RemoteException;
	
}
