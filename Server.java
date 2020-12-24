import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server extends UnicastRemoteObject implements RMIRegister{

	private static final long serialVersionUID = 1L;
	private JSONObject UtentiRegistrati; //JSONObject che contiene gli utenti registrati
	final String FileRegistrazioni = "UtentiRegistrati.json"; //file json degli utenti registrati
	private HashMap<String, SocketChannel> UtentiLoggati; //treemap che contiene gli utenti online e il corrispettivo canale
	final String  FileAmici = "Amici.json"; //file json che contiene le corrispondenze utente-lista amici
	private JSONObject Amici; //jsonObject che contiene utente: jsonarray amici
	final String FilePunteggi = "Punteggi.json"; //file json che contiene le corrispondenze utente-punteggio
	private JSONObject Punteggi; //jsonobject che contiene utente:punti
	private HashMap<String,String> utentiSfida;//hashmap dove salvo l'utente con una stringa che mi controlla se è libero o occupato di ricevere una richiesta di sfida 
														//lo inserisco al momento del login, se già presente lo sovrascrivo
	
	//costruttore
	protected Server() throws RemoteException {
		super(); 
		
		//creo i file degli utenti registrati e degli amici se non esistono, altrimenti recupero i dati dal file già esistente
		File file = new File(FileRegistrazioni);
		if(file.exists()) {
			UtentiRegistrati = recuperoDati(FileRegistrazioni);
		}
		else {
			 UtentiRegistrati = new JSONObject();
		}
		scriviFileJSON(UtentiRegistrati,FileRegistrazioni);
		
		File file1 = new File(FileAmici);
		if(file1.exists()) {
			Amici = recuperoDati(FileAmici);
		}
		else {
			Amici = new JSONObject();
			
		}
		scriviFileJSON(Amici,FileAmici);
		
		//ogni volta che il server si accende creo la struttura dove tengo gli utenti online
		UtentiLoggati = new HashMap<String, SocketChannel>();
		//ogni volta che il server si accende creo la hashmap in cui salvo gli utenti loggati e una stringa che indica il loro stato in base alla loro disponibilità
		//di ricevere una richiesta di sfida
		utentiSfida = new HashMap<String, String>();
	}
	
	//metodo che rimuove un utente dalla struttura dati degli utenti loggati
	public void removeLoggati(String nome) {
		UtentiLoggati.remove(nome);
	}
	
	//funzione synchronized che controlla se un utente è sfidabile
	public synchronized Boolean IsSfidabile(String nome) {
		if (utentiSfida.get(nome).equals("not_ready")){
			return false;
		}
		else {
			utentiSfida.put(nome, "not_ready");
			return true;
		}
	}
	
	//funzione synchronized che rimette un utente a libero
	public synchronized void libero(String nome) {
		utentiSfida.put(nome, "ready");
	}
	
	//metodo usato per recuperare dati da un file e restituirli come file json
	private JSONObject recuperoDati(String NomeFile) {
		
		//inizializzo la variabile obj cosi da poter fare la return dopo le catch
		Object obj=null;
		
		try{
			//creo il canale con il file da leggere in modalità lettura
			FileChannel inChannel= FileChannel.open(Paths.get(NomeFile), StandardOpenOption.READ);
			//recupero la dimensione del file
			int dim=(int)inChannel.size();
			
			//alloco il buffer diretto della dimensione del file +1
			ByteBuffer buffer= ByteBuffer.allocateDirect(dim+1);
			//setto la variabile flag per uscire dal while
			boolean stop=false;
			//leggo tutti i byte dal canale, esco solo quando mi dà byteLetti == -1
			while(!stop) {
				int byteRead= inChannel.read(buffer);
				if(byteRead==-1) {
					stop=true;
				}
			}
			//chiudo il canale
			inChannel.close();
			//setto il buffer in modalità lettura, la posizione mi riparte da zero
			buffer.flip();
			//decodifico da byte a char 
			CharBuffer BufferChar=StandardCharsets.US_ASCII.decode(buffer);

			//trasformo in stringa il buffer
			String stringaDati=BufferChar.toString();
		      
			//faccio il parse per recuperare i dati Json
			JSONParser parser= new JSONParser();
			obj=parser.parse(stringaDati);	        
						
		}
		catch(IOException e) {
			e.printStackTrace();  
			System.exit(1);
			
		}
		catch(ParseException e1) {
			e1.printStackTrace();  
			System.exit(1);
		}
			
		//casto l'oggetto in JsonObject e ritorno
		return (JSONObject) obj;

	}
	
	//metodo usato per scrivere su un file nuovo o per aggiornare uno già esistente avendo i dati su un file json e il nome del file
	//synchronized perchè è utilizzato per scrivere sul file dei punteggi anche dal thread che gestisce la sfida lato server
	public synchronized Boolean scriviFileJSON(JSONObject DatiJSON, String NomeFile) {
        
        try {
  
			
			//creo il canale per scrivere sul file
			FileChannel outChannel= FileChannel.open(Paths.get(NomeFile),StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			//creo un array di byte partendo dalla stringa dell'oggetto Json
			byte[] bytes = DatiJSON.toJSONString().getBytes("UTF-8");
			//alloco un buffer direct da passare al canale
			ByteBuffer buffer=ByteBuffer.allocateDirect(bytes.length); 
			//incapsulo l'array di byte nel buffer
			buffer = ByteBuffer.wrap(bytes);
        
			//vado a scrivere dal buffer al file passando dal canale
			while(buffer.hasRemaining()) {
				outChannel.write(buffer);
			}
        
			//chiudo il canale
			outChannel.close();
			return true;
        }
        
        catch(Exception e1) {
        	e1.printStackTrace();
        	return false;
        }
        
	}

	//metodo che restituisce true se un utente è loggato o false se l'utente non è loggato, andando a controllare se la treemap contiene il nome utente
	public boolean isLogged(String nickUtente) {
		return UtentiLoggati.containsKey(nickUtente);
	}
	
	//metodo invocato quando un utente ha richiesto la registrazione
	@SuppressWarnings("unchecked")
	public synchronized String registra_utente(String nickUtente, String password) {
		
		String esito; //stringa in cui salvo l'esito della registrazione per comunicarlo all'utente
		
		//controllo che i campi non siano vuoti
		if(nickUtente.equals("") || password.equals(""))
			esito = "errore campo vuoto .";
		
		//controllo che l'utente non sia già registrato (quindi che il JSONObject non contenga l'utente), o che comunque un altro utente non utilizzi lo stesso nome di un utente già registrato
		else if(UtentiRegistrati.containsKey(nickUtente) == true) {
			esito = "utente già presente .";
		}
		
		else {//se i campi inseriti sono validi
			
			UtentiRegistrati.put(nickUtente, password); //l'utente viene inserito solo se non è già registrato
			scriviFileJSON(UtentiRegistrati,FileRegistrazioni); //si aggiorna il file json con la nuova coppia utente:pswd			
			esito = "Registrazione avvenuta con successo .";
		}
		
		return esito;
		
	}
	
	//metodo invocato quando un utente ha richiesto un'operazione di login
	public String login(String nickUtente, String password, SocketChannel socket) {
		
		String esito = null;//stringa in cui salvo l'esito della login per comunicarlo all'utente
		
		//controllo che i campi nickUtente e pswd non siano vuoti
		if(nickUtente.equals("") || password.equals("")) {
			esito = "Errore campo vuoto .";
			return esito;
		}
		
		//controllo che l'untente non sia già loggato
		else if(UtentiLoggati.containsKey(nickUtente)) {
			esito = "utente già loggato .";
			return esito;
		}
		
		//controllo che l'utente sia registrato e che la pswd inserita coincida con quella inserita al momento della registrazione
		//altrimenti comunico un messaggio di errore
		else if(UtentiRegistrati.containsKey(nickUtente)) {
			String pswd = (String) UtentiRegistrati.get(nickUtente); //prendo la password dal jsonobject degli utenti registrati
			if(pswd.equals(password)) {
				//se la password è corretta 
				UtentiLoggati.put(nickUtente, socket);//metto l'utente con il corrispettivo canale nella treemap degli utenti loggati
				libero(nickUtente);//metto l'utente nella struttura dati utentiSfida segnalando con una stringa che l'utente è disponibile per le richieste di sfida
				esito = "login effettuato con successo .";
				return esito;
			}
			else { //password errata
				esito = "password errata .";
			return esito;
			
			}
		}
		else {//unica altra cosa non controllata è che l'utente non sia registrato
			esito = "utente non registrato .";
			return esito;
		
		}
		
	}
	
	//metodo invocato quando un utente desidera fare il logout
	public void logout(String nickUtente) {
		//provo a rimuovere l'utente dalla hashmap
		removeLoggati(nickUtente);
	}
	
	
	//nel caso in cui si interrompesse la comunicazione il server lancia un'eccezione e il client deve comunque terminare in modo consistente
	public void logoutEXC(SocketChannel channel) {

		
		//prendo un set di stringhe che sono le chiavi della hashmap
		Set<String> key=UtentiLoggati.keySet();
		
		//con la chiave accedo ai valori della hashmap e la scorro finchè non trovo il nome dell'utente il cui canale corrisponde
		//a quello che si è disconnesso
		for(String nome:key) {
			if(channel.toString().equals(UtentiLoggati.get(nome).toString())) {
				UtentiLoggati.remove(nome);//rimuovo dai loggati l'utente disconnesso
				return;
			}
		}
	}
	
	//metodo invocato quando un utente richiede un'operazione di aggiungi amico
	@SuppressWarnings("unchecked")
	public String aggiungi_amico(String nickUtente, String nickAmico) throws IOException {
		
		String esito = null;//stringa in cui salvo l'esito dell'operazione per comunicarlo all'utente
		boolean ok = false;//esito dell'operazione con cui controllo se l'amico è stato aggiunto con le operazioni sui jsonObject
		System.out.println(nickAmico);
		//controllo che l'utente sia loggato per poter richiedere l'operazione
		if(!isLogged(nickUtente)) {
			esito = "E' necessario eseguire il login .";
			return esito;
		}
		
		//controllo che il campo dell'amico non sia vuoto
		if(nickAmico.equals("")) {
			esito = "Errore campo vuoto .";
			return esito;
		}
		
		//controllo che l'utente non stia inviando una richiesta di amicizia a se stesso
		if(nickUtente.equals(nickAmico)) {
			esito = "Non puoi inviare una richiesta di amicizia a te stesso .";
			return esito;
		}
			
		//controllo che gli utenti siano entrambi registrati
		if (!UtentiRegistrati.containsKey(nickAmico)) {
			esito = "E' necessario che l'amico sia registrato .";
			return esito;
		}
			
		//se il jsonobject degli amici non contiene nickutente significa che non ha ancora aggiunto nessun amico quindi intanto inserisco nickUtente con un JSONArray di amici vuoto
		if(Amici.containsKey(nickUtente) == false) {
			JSONArray temp = new JSONArray();
			Amici.put(nickUtente, temp);
		}
		
		JSONArray Amicizie = (JSONArray) Amici.get(nickUtente); //ottengo il riferimento al jsonarray degli amici di nickUtente
		
		//se nickAmico non è già presente tra gli amici di nickUtente lo aggiungo, riaggiungo poi anche il jsonarray degli amici di nickutente al json obkject degli amici
		if(!Amicizie.contains(nickAmico)) {
			ok = Amicizie.add(nickAmico);
			Amici.put(nickUtente, Amicizie); 
		}
		else { //altrimenti comunico all'utente che sta cercando di aggiungere un amico che già ha
			esito ="Amico già presente .";
			return esito;
		}

		//controllo l'esito dell'operazione di aggiunta dell'utente e dell'amico al JSONArray dei suoi amici e lo comunico all'utente
		if(ok == false) esito = "Errore, " + nickAmico + " non è stato aggiunto. ";
		else esito = "Amico aggiunto con successo .";
		
		if(Amici.containsKey(nickAmico) == false) {
			JSONArray temp = new JSONArray();
			Amici.put(nickAmico, temp);
		}
		
		//faccio lo stesso procedimento anche per l'amico
		JSONArray Amicizie2 = (JSONArray) Amici.get(nickAmico);
		
		if(!Amicizie2.contains(nickUtente)) {
			Amicizie2.add(nickUtente);
			Amici.put(nickAmico, Amicizie2);

		}
		scriviFileJSON(Amici,FileAmici); //aggiorno il file json dove tengo le liste degli amici degli utenti
	
		return esito;
		
	}
	
	//metodo invocato quando un utente richiede la lista dei propri amici
	@SuppressWarnings("unchecked")
	public JSONObject lista_amici(String nickUtente) {
		
		//controllo se l'utente è loggato
		if(!isLogged(nickUtente)) {
			return null;
		}
		
		//se l'utente non ha amici (quindi non è ancora stato inserito nel JSONObject degli amici) lo inserisco nel file json degli amici con l'array delle amicizie vuoto
		if(Amici.containsKey(nickUtente)==false) {
			JSONArray temp = new JSONArray();
			JSONObject tmp = new JSONObject();
			Amici.put(nickUtente, temp);

			return tmp;//restituisco il JSONArray vuoto 
		}
		
		//salvo in un nuovo JSONObject l'oggetto contenente la lista degli amici dell'utente e lo restituisco
		JSONArray Amicizie = (JSONArray) Amici.get(nickUtente);
		JSONObject ListaAmici = new JSONObject();

		ListaAmici.put(nickUtente, Amicizie); 

		return ListaAmici;
	}
	
	//metodo invocato quando un utente richiede il proprio punteggio 
	public String mostra_punteggio(String nickUtente) {
		
		//recupero il file dei punteggi se esiste altrimenti ne creo uno nuovo
		File file2 = new File(FilePunteggi);
		if(file2.exists()) {
			Punteggi = recuperoDati(FilePunteggi);
		}
		else {
			Punteggi = new JSONObject();
		}
		
		String esito; //stringa in cui salvo l'esito della registrazione per comunicarlo all'utente
		
		//controllo se l'utente è loggato
		if(!isLogged(nickUtente))
			esito = "E' necessario eseguire il login .";
	
		//se nickutente non è ancora stato inserito nel file dei punteggi significa che ancora non ha fatto neanche una partita quindi il suo punteggio sarà 0
		if(Punteggi.containsKey(nickUtente) == false) {
			esito = "0 .";
		}
		else {
			//prendo dal jsonobject dei punteggi il punteggio relativo a nickUtente
			esito = String.valueOf(Punteggi.get(nickUtente)) + " .";		
		}
		
		return esito;
			
	}

	//metodo invocato quando un utente richiede la classifica con se stesso e i suoi amici 
	@SuppressWarnings("unchecked")
	public JSONObject mostra_classifica(String nickUtente) {
		
		//recupero il file dei punteggi se esiste altrimenti ne creo uno nuovo
		File file2 = new File(FilePunteggi);
		
		if(file2.exists()) {
			Punteggi = recuperoDati(FilePunteggi);
		}
		else {
			Punteggi = new JSONObject();
		}
		
		//controllo se è loggato
		if(!isLogged(nickUtente))
			return null;
		
		//se l'utente non ha amici restituisco null perchè la classifica sarà vuota
		if(Amici.containsKey(nickUtente)==false) {
			return null;
		}
		
		//ottengo l'array delle amicizie relativo all'utente perchè la classifica non è persistente, cambia ogni volta che un utente fa una sfida
		//quindi ogni volta vanno recuperati amici e punteggi e ordinati per punteggio decrescente
		JSONArray Amicizie = (JSONArray) Amici.get(nickUtente);
		ArrayList<utentiPunti> nomepunti = new ArrayList<utentiPunti>(); //creo un arraylist di elementi di tipo utentiPunti
		
		int i;
		int p; // variabile dove salvo il punteggio di ogni utente
		String amico = ""; // variabile dove salvo il nome di ogni utente
		
		//per ogni amico recupero il punteggio e il nome utente e li inserisco nell'arraylist
		for(i=0;i<Amicizie.size();i++) {
			amico = (String) Amicizie.get(i); //recupero nome utente
			//se l'utente recuperato è presente nel file dei punteggi recupero il punteggio
			if(Punteggi.containsKey(amico))
				p = Integer.parseInt(String.valueOf(Punteggi.get(amico)));
			else p = 0; //altrimenti significa che non ha ancora fatto nessuna sfida e quindi ha 0 punti
			//aggiungo all'array list l'utente con i suoi punti mettendoli in un nuovo elemento di tipo utentipunti
			utentiPunti el = new utentiPunti(amico,p); 
			nomepunti.add(el);
		}
		
		//devo inserire nella classifica anche l'utente che ha richiesto di visualizzare la sfida 
		int puntiUser = 0; // inizializzo a 0 così se non è presente nel file dei punteggi è già settato al punteggio giusto
		
		//recupero il suo punteggio dal file dei punteggi se ha mai giocato una sfida e quindi è presente nel file
		if(Punteggi.containsKey(nickUtente)) {
			puntiUser = Integer.parseInt(String.valueOf(Punteggi.get(nickUtente)));
		}
		//aggiungo un nuovo elemento all'arraylist da ordinare un elemento di tipo utentipunti con nome nickutente e punteggio quello appena recuperato
		nomepunti.add(new utentiPunti(nickUtente,puntiUser));
	
		Collections.sort(nomepunti);//ordino l'arraylist secondo il metodo compare to della classe utentipunti quindi per punteggio decrescente

		JSONObject classificaAmici = new JSONObject(); //creo un nuovo jsonobject da restituire contenente la classifica degli amici di nickutente
		JSONArray classif = new JSONArray(); //json array contenente la classifica
		
		//faccio un ciclo che scorre tutto l'arraylist ordinato e prendo un nome utente per volta da inserire nel json array per poi restituirlo all'utente
		for(i=0;i<nomepunti.size();i++) {
			utentiPunti el = nomepunti.get(i); //recupero dall'arraylist ogni elemento di tipo utentipunti
			String us = el.getName(); //prendo il nome dell'utente di quell'elemento
			int pt = el.getPt(); //prendo i punti dell'utente di quell'elemento
			
			JSONObject temp = new JSONObject();
			temp.put(us, pt); //inserisco nel jsonobject la coppia utente punti
			classif.add(temp); // inserisco il jsonobjhect nel jsonarray
		}
		
		classificaAmici.put(nickUtente, classif); //inserisco il jsonarray nel jsonobject e lo ritorno
	
		return classificaAmici;
		
	}
	
	//meotodo invocato quando un utente ha richiesto di avviare una sfida con un amico per controllare che i parametri siano corretti
	public boolean sfida(String nickUtente, String nickAmico, SocketChannel socketTCP) throws IOException {
		
		//nickutente e nickamico devono essere loggati
		if(!isLogged(nickUtente) || !isLogged(nickAmico)) {
			return false;	
		}
		
		//prendo il riferimento al JSONArray degli amici 
		JSONArray amici = (JSONArray) Amici.get(nickUtente);
		
		//controllo che nickamico al quale si vuole inviare la richiesta di sfida sia nella lista di amici dell'utente e faccio ritornare un booleano 
		if(!amici.contains(nickAmico)){
			return false;
		}
		return true;
	}

	//metodo con cui controllo le stringhe di richiesta del client e chiamo i metodi relativi alle richieste
	public String analisiMess(String datiLetti, ByteBuffer message, SocketChannel client, Selector selector) throws IOException {
		
		//tokenizzo la stringa dei datiletti
		String[] strTOK = datiLetti.split(" ");
		String p1 = strTOK[0]; //nella prima parola c'è l'operazione richiesta dal client
		String ris = ""; //stringa con il risultato dell'operazione richiesta da restituire al client
		boolean res; //boolean per analizzare il risultato restituito dalla sfida
		JSONObject temp = new JSONObject(); //JSONObject dove salvare la lista amici o la classifica
		
		//se l'operazione richiesta è login
		if(p1.equals("Login")) {
			ris = login(strTOK[1],strTOK[2],client);
		}
		//se l'operazione richiesta è logout
		else if(p1.equals("Logout")) {
			logout(strTOK[1]);
			ris = "logout effettuato con successo .";
		}
		//se l'operazione richiesta è aggiungi amico
		else if(p1.equals("Aggiungi")) {
			ris = aggiungi_amico(strTOK[1], strTOK[2]);
		}
		//se l'operazione richiesta è aggiungi amico
		else if(p1.equals("Lista_amici")) {
			temp = lista_amici(strTOK[1]);
			
			//controllo che la lista amici restituita non sia null altrimenti lo comunico all'utente
			if(temp == null) {
				ris = "Errore nel recupero della lista amici .";
			}
			else {
				ris = temp.toJSONString() + " .";
			}
		}
		//se l'operazione richiesta è mostra punteggio
		else if(p1.equals("Punteggio")) {
			ris = mostra_punteggio(strTOK[1]);
		}
		//se l'operazione richiesta è mostra classifica
		else if(p1.equals("Mostra_classifica")) {
			temp = mostra_classifica(strTOK[1]);
			
			//controllo che la classifica restituita non sia null altrimenti lo comunico all'utente
			if(temp == null) {
				ris = "Errore nel recupero della classifica .";
			}
			else ris = temp.toJSONString() + " .";
		}
		//se l'operazione richiesta è una sfida
		else if(p1.equals("Sfida")) {
			
			//chiamo il metodo che verifica che i paramentri per inviare una richiesta di sfida siano corretti
			 res = sfida(strTOK[1],strTOK[2],client);
			 
			 if(res == false) {
				 //se non sono corretti lo comunico al client
				 ris = "Sfida rifiutata errore sui parametri ."; 
			 }
			 else {
				 
				 //se sono corretti setto subito il richiedente della sfida a occupato nella struttura dati utenti sfida
				 utentiSfida.put(strTOK[1],"not_ready");
				 
				 //lancio il thread che si occupa della sfida lato server con i due utenti, i loro canali, il selettore e la struttura dati
				 //utentisfida per settare a occupato l'utente sfidato nel mentre riceve la richiesta o eventualmente durante la sfida se la accetta
				 sfidaS ss = new sfidaS(strTOK[1],strTOK[2],UtentiLoggati.get(strTOK[1]),UtentiLoggati.get(strTOK[2]),selector, utentiSfida,this, UtentiLoggati);
				 Thread t = new Thread(ss);
				 t.start();
			 }
		}
		else {
			ris = "Errore comando inserito non supportato ."; //comando non tra quelli sopra
		}
		return ris;
	}
	
}
