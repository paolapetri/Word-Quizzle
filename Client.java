import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Client {
	private SocketChannel socketTCP; //socket con cui si connette con il server
	private InetAddress indServer; //indirizzo server (ip+porta)
	private boolean isLogged; //si mette a true quando un client si logga, così se non è loggato si interrompe subito l'operazione che desiderava eseguire senza aver fatto prima login
	private int portServer; //porta del server
	private int portRMI; //porta RMI
	private gestoreSfida clientUDP; //salvo il tid in una variabile per poter stoppare il thread quando un client fa logout
	private boolean started; //variabile booleana utilizzata per avviare il thread di gestione della sfida una volta sola, inizializzata a false e messa a true dopo l'avvio
	
	//metodo costruttore
	public Client(InetAddress indserver2, int portServer, int portRMI) throws SocketException{
		this.indServer = indserver2;
		this.portServer = portServer;
		this.portRMI = portRMI;
		this.isLogged = false;	
		this.started=false;
	}
	
	//metodo utilizzato per scrivere messaggi sulla socket tcp quindi al server
	public void scriviMess(String mess) throws IOException {
		//buffer di 512 bytes dove incapsulare il messaggio passato come argomento alla funzione 
		ByteBuffer bufMess = ByteBuffer.allocateDirect(512);
		//incapsulo il messaggio codificandolo con utf-8
		bufMess = ByteBuffer.wrap(mess.getBytes("UTF-8"));
		//scrivo finchè ci sono dati nel buffer
		while(bufMess.hasRemaining()) {
			socketTCP.write(bufMess);
		}
		//faccio la clear sul buffer
		bufMess.clear();
	}
	
	//metodo utilizzato per leggere messaggi dalla socket tcp quindi dal server
	public String leggiMess() throws IOException {
		//buffere di 512 bytes dove salvare il messaggio letto
		ByteBuffer bufRisp = ByteBuffer.allocateDirect(512);
		
		Boolean stop = false; //variabile booleana che uso per leggere finchè non diventa true, quindi quando i dati sono finiti
		String datiLetti = "";
		
		while(!stop) {

			bufRisp.clear();//a ogni ciclo faccio la clear sul buffer in caso di più letture 
			socketTCP.read(bufRisp); //leggo

			bufRisp.flip();//porto la posizione del buffer a 0
			
			//decodifico quello che ho letto e lo inserisco in una stringa dove concateno i risultati di tutte le letture
			CharBuffer bufRispChar = StandardCharsets.UTF_8.decode(bufRisp);
			datiLetti = datiLetti + bufRispChar.toString();
			
			//mi fermo quando trovo il punto come segnale di fine messaggio
			if(datiLetti.endsWith("."))
				stop = true;			
		}

		//ritorno la stringa letta
		return datiLetti;
	}

	//metodo che lancia il thread che sta in attesa della richiesta di una sfida da parte di un altro client
	public void attesaSfida(guiLogged guiLogged) throws SocketException {
		
		//si chiama solo se no è già partito il thread perchè la chiamata di qywsto metodo avviene ogni volta
		//che si passa alla schermata principale dell'interfaccia, controllando la variabile started
		if(this.started == false) {
			this.clientUDP = new gestoreSfida(socketTCP, guiLogged);
			Thread T = new Thread(this.clientUDP);
			T.setDaemon(true); //setto il thread come demone
			T.start();//viene attivato al momento della login e muore con la terminazione del thread del client
			this.started=true;//setto la variabile a true in modo che ogni volta che viene invocato questo metodo non verrà ri lanciato il thread
		}
		
	}

	//metodo invocato quando un utente desidera fare la registrazione al sistema
	public String registra_utente(String nickUtente, String password) throws NotBoundException {
		String ServiceName = "registraUtente"; //nome per il riferimento nel registro
		String esito = ""; //stringa per restituire l'esito dell'operazione
			try {
				Registry reg = LocateRegistry.getRegistry(portRMI); //riferimento al registro nella prota RMI
				RMIRegister registro = (RMIRegister) reg.lookup(ServiceName); //riferimento all'oggetto nel registro
				String ok = registro.registra_utente(nickUtente, password); //chiamo la funzione e registro l'utente
				if(ok.equals("Registrazione avvenuta con successo ."))
						esito = "Registrazione effettuata con successo";
				else esito ="Registrazione non riuscita " + ok; //in caso di esito negativo, concateno la stringa con la motivazione
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		return esito;
	}
	
	//metodo invocato quando un utente desidera fare il login
	public String login(String nickUtente, String password) throws IOException {

		String rispostalog = "";//stringa per restituire l'esito dell'operazione
		
		//caso in cui un utente provi a loggarsi due volte al sistema
		if(isLogged == true) {
			rispostalog = "Utente già loggato";
			return rispostalog; //ritorno direttamente all'utente una stringa di errore senza connettermi con il server
		}
		
		//si apre la socket per connettersi con il server e richiedere la login al sistema
		SocketAddress sa = new InetSocketAddress(indServer, portServer);
		socketTCP = SocketChannel.open(sa);
		socketTCP.configureBlocking(true);
		
		//Stringa da inviare al server con il messaggio di richiesta di login
		String log ="";
		log = "Login " + nickUtente + " " + password + " .";
		scriviMess(log);
		
		//stringa in cui salvo il messaggio di risposta del server
		rispostalog = leggiMess();
		
		//se la login va a buon fine chiamo il metodo attesa sfida che si occupa di attendere una sfida dal momento in cui un utente è loggato e può ricevere richieste
		if(rispostalog.equals("login effettuato con successo .")) {
			//attesaSfida();
			isLogged = true;//metto la variabile che controlla se un utente è loggato a true
		}
		
		return rispostalog;
	}
	
	//metodo invocato quando un utente desidera fare logout
	public void logout(String nickUtente) throws IOException {
		if(isLogged == false) {
			return;
		}
		//Stringa da inviare al server con il messaggio di richiesta di logout
		String log = "Logout " + nickUtente + " .";
		scriviMess(log);
		
		isLogged = false;//metto la variabile che controlla se un utente è loggato a false
		
		socketTCP.close();//chiudo la socket tcp 	
	}
	
	//metodo invocato quando un utente desidera aggiungere un nuovo amico
	public String aggiungi_amico(String nickUtente, String nickAmico) throws IOException {
		
		String ris = "";//stringa per restituire l'esito dell'operazione
		
		if(!isLogged) {
			return null;
		}
		
		//Stringa da inviare al server con il messaggio di richiesta di aggiungere l'amico nickAmico, specificando anche il nome dell'utente che la richiede
		String add = "Aggiungi " + nickUtente + " " + nickAmico + " .";
		scriviMess(add);
		
		//salvo nella stringa risp l'esito restituito del server
		String risp = leggiMess();
		
		//preparo l'esito da restituire all'utente eventualmente togliendo spazi e punti finali
		if(risp.equals("Amico aggiunto con successo .")) {
			ris = "Amico aggiunto con successo!";
		}
		else ris = risp.substring(0,risp.length()-2);
		
		return ris;
	}

	//metodo invocato quando un utente desidera visualizzare la sua lista amici
	public JSONObject lista_amici(String nickUtente) throws IOException, ParseException {
		
		if(!isLogged) {
			System.out.println("non sei ancora loggato .");
			return null;
		}
		
		//Stringa da inviare al server con il messaggio di richiesta di visualizzare la lista amici
		String req = "Lista_amici " + nickUtente + " .";
		scriviMess(req);
		//salvo nella stringa risp quello che mi ha restituito il server
		String risp = leggiMess();
		
		//se l'utente non ha amici restituisco null
		if(risp.equals("{} .")) {
			return null;
		}
		
		//se l'utente ha amici allora trasformo la stringa in un JSONObject e la restituisco
		risp = risp.substring(0,risp.length()-1);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(risp);
		JSONObject Amici = (JSONObject) obj;
		
		return Amici;
	}
	
	//metodo invocato quando un utente desidera visualizzare il suo punteggio
	public String mostra_punteggio(String nickUtente) throws IOException {
		if(!isLogged) {
			System.out.println("non sei ancora loggato .");
			return null;
		}
		//Stringa da inviare al server con il messaggio di richiesta di visualizzare il punteggio
		String req = "Punteggio " + nickUtente + " .";
		scriviMess(req);
		
		//salvo nella stringa risp il punteggio restituito del server
		String risp = leggiMess();
		risp = risp.substring(0,risp.length()-1);//tolgo il punto e ritorno il punteggio come stringa
		return risp;
	}

	//metodo invocato quando un utente desidera visualizzare la classifica
	public JSONObject mostra_classifica(String nickUtente) throws IOException, ParseException {
		
		if(!isLogged) {
			return null;
		}
		
		//Stringa da inviare al server con il messaggio di richiesta di visualizzare la classifica
		String req = "Mostra_classifica " + nickUtente + " .";
		scriviMess(req);
		//salvo nella stringa risp l'esito restituito del server
		String risp = leggiMess();
		
		risp = risp.substring(0,risp.length()-1); //tolgo il punto
		
		if(!risp.equals("Errore nel recupero della classifica ")) {
			//caso in cui l'utente abbia amici e quindi il server sia riuscito a recuperare la classifica 
			//trasformo la stringa in un jsonobject e la ritorno
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(risp);	
			return obj;
		}
		
		else {
			//caso in cui l'utente non abbia amici e la classifica non possa essere restituita, restituisco null
			return null;
		}
	}
	
	//metodo invocato quando un utente desidera avviare una sfida con un amico
	public void Sfida(String nickUtente, String nickAmico, guisfida guisfida) throws IOException, InterruptedException {
		
		if(!isLogged) {
			return ;
		}
		
		//Stringa da inviare al server con il messaggio di richiesta di inviare una richiesta di sfida all'utente nickamico
		String req = "Sfida " + nickUtente + " " + nickAmico + " .";
		scriviMess(req);
		
		//salvo nella stringa risp l'esito restituito del server, ovvero quello che ha risposto l'utente sfidato
		String risp = leggiMess();
		
		//chiamo il metodo della finestra di sfida che cambia in base alla risposta ricevuta la schermata allo sfidante
		guisfida.cambiaFinestra(risp);
		
	}
	
}
