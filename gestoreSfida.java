import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

//classe del thread che sta in attesa delle richieste di sfida e gestisce la sfida lato client che ha ricevuto la richiesta
public class gestoreSfida implements Runnable{
	
	private DatagramSocket socketUDP;
	private guiLogged guiLogged;
	
	//metodo costruttore
	public gestoreSfida(SocketChannel socketTCP, guiLogged guiLogged) throws SocketException {
		
		//mi serve la socket tcp per ricavare l'indirizzo del client prendendo l'endpoint della socket e poi passarlo al costruttore della socketudp
		Socket IndirizzoClient=socketTCP.socket();
		int portClient= IndirizzoClient.getLocalPort();
		this.socketUDP = new DatagramSocket(portClient);
		this.guiLogged = guiLogged;
	}
	
	public void run() {
		
		//finchè non è chiamato il metodo interrupt() il thread gira in attesa di richieste, qualora il client fosse occupato in un'altra sfida sarà il server a non inoltrargli richieste
		while(!Thread.interrupted()) {
			
			System.out.println("in attesa di richieste di sfida..");
			
			//mi metto in attesa di ricevere un messaggio UDP, perchè quello che il server inoltra allo sfidato è in UDP
			byte[] ba = new byte[512];// preparo byte array e datagram packet dove salvarlo
			DatagramPacket dp = new DatagramPacket(ba, 512);
			
			try {
				socketUDP.receive(dp); //sono in attesa sulla receive


			} catch (IOException e) {
				
				if(e.getMessage().equals("Socket closed")) {
					//se l'eccezione è la chiusura della socket chiudo il thread perchè è terminato
					return;
				}
				
			}
				String req = "";//stringa dove salvare il messaggio letto dalla socket udp
			try {
				req = new String(ba, "UTF-8"); //trasformo in stringa quello che ho letto dal byte array
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			String[] tokenizzate = req.split(" "); //tokenizzo la stringa letta
			
			//nel caso arrivi una richiesta di sfida, prendo dalla stringa letta di richiesta il nome dell'amico che mi ha sfidato 
			String amico="";
			amico = tokenizzate[2]; //la stringa sarà "sfida nickutente nickamico"
			
			//chiamo il metodo della schermata principale dell'interfaccia che mi crea una schermata dove posso accettare o meno la sfida
			guiLogged.sfidato(amico);
			
		}
		
		
		
	}
	
}
