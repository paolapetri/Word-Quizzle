import java.awt.EventQueue;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;

public class MainClassClient {

	public static void main(String[] args) throws NotBoundException, SocketException {
		
		final int portRMI = 4000; //porta del servizio RMI
		
		final int portServer = 23456; //porta del server
		
		//creo un'inetSocketAddress con ip=localHost e PortServer
		InetAddress indserver = null;
		
		try {
			indserver = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		//lancio un'istanza della classe client
		Client prova = new Client(indserver, portServer, portRMI);
			
		//apro l'interfaccia grafica del client
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						//passo null al parametro del frame perchè la apro per la prima volta 
						guiiniziale window = new guiiniziale(prova, null);
						window.frame.setVisible(true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
	}

}
