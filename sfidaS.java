import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class sfidaS implements Runnable {

	private String us1;// nome dell'utente che ha inviato la richiesta di sfida
	private String us2;// nome dell'utente che è stato sfidato
	private JSONObject Punteggi;// jsonobject dei punteggi
	private String FilePunteggi = "Punteggi.json";// nome del file json dei punteggi

	// numero di parole da inserire nell'arraylist di parolePunti per poi inviarle
	// agli utenti
	private int nParole1;
	private int nParole2;
	private ArrayList<parolePunti> parole1;
	private ArrayList<parolePunti> parole2;

	// variabili booleane che il server setta a true se un utente si disconnette (una per ogni utente)
	private boolean disconnesso1 = false;
	private boolean disconnesso2 = false;
	
	private int k; // numero di parole da tradurre
	private SocketChannel c1;// canale dell'utente che ha inviato la richiesta di sfida
	private SocketChannel c2;// canale dell'utente che è stato sfidato

	private static String DIZ = "parole.txt";// nome del file delle parole italiane da prendere casualmente e inviare
												// agli utenti
	private String stringaDiz; // variabile in cui mi salvo il contenuto del dizionario per tokenizzarla e
								// dividere le parole una ad una
	private Selector selectorM;// selettore del mainserver dal quale devo togliere le chiavi dei due client che
								// stanno facendo la sfida
	
	private Server server;// istanza del server
	private boolean uscita = false; //variabile booleana settata a true per interrompere le operazioni del server nel caso un client abbia avuto problemi e si sia disconnesso

	// costruttore
	public sfidaS(String us1, String us2, SocketChannel c1, SocketChannel c2, Selector selectorM,
			HashMap<String, String> utentiSfida, Server server, @SuppressWarnings("rawtypes") HashMap U) {

	
		this.c1 = c1;
		this.c2 = c2;
		this.server = server;
		
		// tolgo le chiavi dal selettore del main
		SelectionKey k1 = c1.keyFor(selectorM);
		SelectionKey k2 = c2.keyFor(selectorM);
		k1.interestOps(0);
		k2.interestOps(0);

		nParole1 = 0;
		nParole2 = 0;
		this.us1 = us1;
		this.us2 = us2;

		k = 8;// sono 8 parole da tradurre


		parole1 = new ArrayList<parolePunti>();
		parole2 = new ArrayList<parolePunti>();

		openFile();// apro il file contenente il dizionario

		// apro il file dei punteggi se esiste recupero i dati altrimenti ne creo uno
		// nuovo
		File file2 = new File(FilePunteggi);
		if (file2.exists()) {
			Punteggi = recuperoDati(FilePunteggi);
		} else {
			Punteggi = new JSONObject();
		}

		this.selectorM = selectorM;
	}

	// metodo chiamato nel costruttore per aprire il file che contiene il dizionario
	// con le parole italiane da inviare al server
	public void openFile() {
		FileChannel o = null;
		ByteBuffer buf = null;
		try {
			// apro con un file channel il dizionario in lettura
			o = FileChannel.open(Paths.get(DIZ), StandardOpenOption.READ);
			int dim = (int) o.size();

			// alloco un bytebuffer della dimensione del filechannel + 1
			buf = ByteBuffer.allocateDirect(dim + 1);
			boolean fine = false;

			// leggo fino a che la read non mi ritorna -1 che significa che non ci sono più
			// byte da leggere
			while (!fine) {
				int byteLetti = o.read(buf);
				if (byteLetti == -1) {
					fine = true;
				}
			}

			// faccio la flip del buffer
			buf.flip();
			o.close();// chiudo il Filechannel

		} catch (IOException e) {
			e.printStackTrace();
		}

		CharBuffer cbuf = StandardCharsets.UTF_8.decode(buf);// decodifico il buffer con utf-8 trasformandolo in char
																// buffer
		stringaDiz = cbuf.toString();// dal charbuffer posso trasformare in stringa il contenuto del dizionario letto
	}

	// metodo chiamato quando si inseriscono le parole italiane prese a caso dal
	// dizionario nell'arrayList per poi inviarle agli utenti per controllare che
	// casualmente non
	// si scelgano parole uguali per una stessa sfida
	public boolean controlloDuplicati(ArrayList<parolePunti> array, String parola) {
		int i;
		for (i = 0; i < array.size(); i++) {

			if (array.get(i).getParolaITA().equals(parola))
				return true;// restituisco true se all'interno dell'array nel quale inserisco le parole, tra
							// le parole già inserite ce n'è una uguale a quella che sto tentando di
							// inserire
		}
		return false; // altrimenti false e posso inserire la parola
	}

	// metodo chiamato per recuperare la traduzione di una determinata parola e
	// verificarne la correttezza
	private String translate(String parola) throws IOException, ParseException {

		URL serverTraduzione = new URL("https://api.mymemory.translated.net");

		String richiestaTraduzione = "/get?q=" + parola + "&langpair=it|en"; // per la traduzione della parola da
																				// italiano a inglese
		URL URL_Completa = new URL(serverTraduzione, richiestaTraduzione); // creo la nuova url che conterra la parola
																			// da tradurre

		URLConnection httpConn = URL_Completa.openConnection(); // apro la connessione all'oggeto specificato dal'url
		httpConn.connect(); // apro effetivamente la connessione

		BufferedReader inReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream())); // apro un
																										// reader per
																										// leggere i
																										// dati dal
																										// servizio di
																										// traduzione

		// Faccio la lettura della roba che mi invia il server di traduzione
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = inReader.readLine()) != null) {
			sb.append(line);
		}

		String tmp_s = new String(sb); // creo una stringa dallo StringBUffer

		JSONParser parser = new JSONParser();
		JSONObject jo = (JSONObject) parser.parse(tmp_s); // oggetto json di risposta alla richesta di traduzione

		// recupero la traduzione
		JSONObject jj = (JSONObject) jo.get("responseData");
		String traduzione = (String) jj.get("translatedText");

		return traduzione;
	}

	// metodo usato per standardizzare le stringhe e renderle uguali per non fare
	// errori nel confronto delle traduzioni
	private String stdString(String parola) {
		parola = parola.trim(); // tolgo gli eventuali spazi
		parola = parola.toLowerCase(); // porto tutto a minuscolo
		parola = parola.replaceAll("[\\-,\\^,\\.,\\,\\;,\\@,\\!,\\%,\\&,\\',\\:,\\?,\\#,\\+,\\*]", ""); // sostituisco a
																										// tutti i
																										// simboli il
																										// cvarattere
																										// vuoto
		return parola;
	}

	// metodo chiamato per recuperare le traduzioni delle parole italiane scelte per
	// la sfida
	private String[] vettoreTrad() throws IOException, ParseException {

		String[] traduzioni = new String[parole1.size()];// array di stringhe dove salvo le traduzioni, avrà la stessa
															// dimensione dell'array delle parole italiane
		// scorro l'arraylist
		for (int i = 0; i < parole1.size(); i++) {
			String parolaITA = parole1.get(i).getParolaITA();// recupero la parola italiana
			String stringaTrad = translate(parolaITA);// chiamo il metodo per recuperare la traduzione corretta
			stringaTrad = stdString(stringaTrad);// standardizzo la stringa
			traduzioni[i] = stringaTrad;// la inserisco nell'array
		}

		return traduzioni;
	}

	// metodo usato per calcolare i punteggi degli utenti
	// gli passo l'arrayList di parolePunti per prendere le traduzioni inviate
	// dall'utente 1/2 e il vettore con le traduzioni corrette
	public String punteggi(ArrayList<parolePunti> array, String[] trad) throws IOException, ParseException {
		int i;
		int vuote = 0;// numero di risposte non date
		int giuste = 0;// numero di risposte corrette
		int sbagliate = 0;// numero di risposte errate

		// scorro l'arrayList
		for (i = 0; i < array.size(); i++) {
			String stringaTrad = trad[i];// prendo la traduzione corretta dal vettore delle traduzioni, le ho salvate in
											// ordine
			String parolaENG = array.get(i).getParolaENG();// prendo dall'arrayList la traduzione inviata dall'utente

			// caso risposta non data
			if (parolaENG.equals("non_data")) {
				array.get(i).setPunti(0);// assegno 0 punti per ogni risposta non data
				vuote++;
			}

			// caso risposta corretta
			else if (stringaTrad.equals(parolaENG)) {
				array.get(i).setPunti(3);// assegno 3 punti per ogni risposta corretta
				giuste++;
			}

			// caso risposta errata
			else {
				array.get(i).setPunti(-1);// tolgo un punto per ogni risposta errata
				sbagliate++;
			}
		}

		String esito = "";// stringa che uso per comunicare l'esito al client
		int tot = 0;// variabile in cui salvo il totale dei punti dell'utente per inserirla nella
					// stringa con l'esito

		// calcolo il totale scorrendo l'arrayList che nel campo punti contiene il
		// punteggio relativo ad ogni risposta
		for (i = 0; i < array.size(); i++) {
			tot = tot + array.get(i).getPunti();// sommo tutti i punti
		}

		// stringa con l'esito da comunicare all'utente e usata dal server per
		// aggiornare il file json dei punti prendendo il totale dei punti
		esito = "Hai tradotto correttamente " + giuste + " parole, ne hai sbagliate " + sbagliate
				+ " e non hai risposto a " + vuote + " hai totalizzato " + tot + " punti .";

		return esito;
	}

	// metodo usato per recuperare dati da un file e restituirli come file json
	private JSONObject recuperoDati(String NomeFile) {

		// inizializzo la variabile obj cosi da poter fare la return dopo le catch
		Object obj = null;

		try {
			// creo il canale con il file da leggere in modalità lettura
			FileChannel inChannel = FileChannel.open(Paths.get(NomeFile), StandardOpenOption.READ);
			// recupero la dimensione del file
			int dim = (int) inChannel.size();

			// alloco il buffer diretto della dimensione del file +1
			ByteBuffer buffer = ByteBuffer.allocateDirect(dim + 1);
			// setto la variabile flag per uscire dal while
			boolean stop = false;
			// leggo tutti i byte dal canale, esco solo quando mi dà byteLetti == -1
			while (!stop) {
				int byteRead = inChannel.read(buffer);
				if (byteRead == -1) {
					stop = true;
				}
			}
			// chiudo il canale
			inChannel.close();
			// setto il buffer in modalità lettura, la posizione mi riparte da zero
			buffer.flip();
			// decodifico da byte a char
			CharBuffer BufferChar = StandardCharsets.US_ASCII.decode(buffer);

			// trasformo in stringa il buffer
			String stringaDati = BufferChar.toString();

			// faccio il parse per recuperare i dati Json
			JSONParser parser = new JSONParser();
			obj = parser.parse(stringaDati);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);

		} catch (ParseException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		// casto l'oggetto in JsonObject e ritorno
		return (JSONObject) obj;

	}

	@SuppressWarnings({ "static-access", "unchecked", "unused" })
	public void run() {

		// --------------------------GESTIONE INVIO MESSAGGIO UDP ALL'UTENTE CHE E' STATO SFIDATO ---------------------------------------------------

		// prima devo verificare se l'utente sfidato è libero di ricevere richieste di
		// sfida, altrimenti neanche gli invio la richiesta

		if (!server.IsSfidabile(us2)) {
			
			// caso in cui l'amico da sfidare sia già occupato , invio all'utente che l'ha
			// sfidato una stringa con l'esito
			String risp = "Sfida rifiutata, utente occupato .";
			ByteBuffer bRisp = ByteBuffer.allocateDirect(512);
			
			// inserisco la stringa nel Bytebuffer
			try {
				bRisp = ByteBuffer.wrap(risp.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			// vado a scrivere sul canale dell'utente che ha inviato la richesta di sfida
			while (bRisp.hasRemaining()) {
				try {
					c1.write(bRisp);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// metto il selettore in lettura con l'utente che aveva richiesto la sfida
			this.c1.keyFor(selectorM).interestOps(SelectionKey.OP_READ);
			this.selectorM.wakeup(); // risveglio il selettore
			return;// esco dal thread perchè la sfida non può partire

		}

		// se non sono entrata in questo if so che l'utente non è occupato e posso
		// provare a chiedergli se vuole accettare la sfida


		// -----------------------------PREPARO LE PAROLE DA INVIARE AGLI UTENTI PER FARE LA SFIDA---------------------------------------------

		// nel dizionario ho ogni parola separata da \r\n
		stringaDiz = stringaDiz.replaceAll("\r\n", "\n");// sostituisco i separatori con \n
		String[] paroleDIZ = stringaDiz.split("\n");// posso ora dividere le parole ogni volta che trovo \n

		int i = 0;

		// con un ciclo (che deve inserire esattamente k parole) inserisco le parole in
		// italiano da fornire agli utenti
		while (i < k) {

			// prendo un numero casuale n in modo da prelevare dal vettore paroleDiz la
			// n-esima parola
			int n = (int) (Math.random() * paroleDIZ.length);
			String parola = paroleDIZ[n];
			// controllo di non aver già inserito una parola all'interno dell'arrayList per
			// non inviare agli utenti due parole uguali
			if (controlloDuplicati(parole1, parola) == false) {
				// creo due elementi di tipo parolePunti
				parolePunti el = new parolePunti();
				parolePunti el1 = new parolePunti();
				// setto la parola italiana dei due elementi
				el.setParolaITA(parola);
				el1.setParolaITA(parola);
				// li inserisco negli arraylist, le parole sono uguali ma uso due arraylist, uno
				// per ogni utente in modo da salvarci
				// successivamente le traduzioni inviate e i rispettivi punteggi assegnati per
				// ogni traduzione (campi che cambieranno da utente a utente)
				parole1.add(el);
				parole2.add(el1);
				i++;
			}

		}

		// -----------------------------ho finito di settare le parole da inviare---------------------------------------------

		// selettore con cui gestisco la sfida
		Selector selector = null;
		paroleDIZ = null;// libero memoria dalla stringa che conteneva le parole del dizionario
		stringaDiz = "";

		try {

			

			// invio la richiesta della sfida in udp
			SocketAddress amico = this.c2.getRemoteAddress();
			DatagramSocket socketUDP = new DatagramSocket();// datagramsocket per inviare il messaggio
			String req = "Sfida da " + us1 + " dovrai tradurre 8 parole in 60 secondi ."; // stringa di richiesta di
																							// sfida
			byte[] stringbuf = req.getBytes("UTF-8");// trasformo la stringa in bytes
			DatagramPacket dp = new DatagramPacket(stringbuf, stringbuf.length, amico); // preparo il datagram packet
			socketUDP.send(dp);// invio il datagram packet
			socketUDP.close();// chiudo la socket udp perchè non ho altri messaggi da inviare o ricevere in
								// udp

			// -------------------------FINE INVIO MESSAGGIO UDP ---------------------------------------------------

			// -------------------------GESTIONE RISPOSTA DELLO SFIDATO DA RICEVERE IN TCP E INOLTRO DELLA RISPOSTA ALL'UTENTE SFIDANTE---------------------------

		}
		catch(Exception e) {
		} 
		
		try {
			// creo il selettore
			selector = Selector.open();
			this.c2.register(selector, SelectionKey.OP_READ); // registro il canale per far rispondere l'utente sfidato
																// alla richiesta di sfida
			//selector.selectedKeys().clear();

			// devo ricevere la risp in tcp quindi uso il selettore e lo metto in attesa
			// sulla select per un timeout
			// salvo l'esito della select che mi ritorna le chiavi pronte entro il timeout
			int ris = selector.select(10000); // 10 sec timeout

			// caso nessuna chiave pronta(non ricevo risposta)
			if (ris == 0) {
				// preparo la stringa che notifica allo sfidante che la sfida non è stata
				// accettata
				String tScaduto = "Sfida non accettata .";

				// invio al client che aveva richiesto la sfida una stringa di esito negativo
				ByteBuffer buftimeout = ByteBuffer.allocateDirect(tScaduto.length() + 1);
				// inserisco la stringa nel Bytebuffer
				buftimeout = ByteBuffer.wrap(tScaduto.getBytes("UTF-8"));
				
				try {
					// vado a scrivere sul canale
					while (buftimeout.hasRemaining()) {
						c1.write(buftimeout);
					}
				}
				catch(Exception e){
					
				}

				// rimetto i due utenti a libero perchè la sfida non è stata accettata e sono
				// disponibili per ricevere nuove richieste di sfida
				server.libero(us1);
				server.libero(us2);


				// rimetto sulla read il selettore del main ai due utenti
				SelectionKey key = c1.keyFor(this.selectorM);
				SelectionKey key2 = c2.keyFor(this.selectorM);
				key.interestOps(SelectionKey.OP_READ);
				key2.interestOps(SelectionKey.OP_READ);
				this.selectorM.wakeup();// risveglio il selettore

				return;// esco dal thread perchè la sfida non deve inizare
			}

			else {
				// caso in cui ho una chiave pronta, mi ha risposto qualcosa

				ByteBuffer bufrisp = ByteBuffer.allocateDirect(512); // alloco un bytebuffer dove memorizzare la
																		// risposta letta
				String risp = "";

				// leggo finchè non trovo il punto
				Boolean fine = false;

				while (!fine) {
					// azzero il ByteBuffer in caso di più letture
					bufrisp.clear();
					// leggo dal canale
					c2.read(bufrisp);
					// ritorno in position 0
					bufrisp.flip();

					// decodifco il messaggio e lo trasformo in stringa
					CharBuffer BufferChar = StandardCharsets.UTF_8.decode(bufrisp);
					risp = risp + BufferChar.toString();

					// controllo se ho letto fino al punto
					if (risp.endsWith(".")) {

						fine = true;
					}
				}

				// tolgo lo spazio e il punto finali
				risp = risp.substring(0, risp.length() - 2);
				//System.out.println("risposta alla richiesta: " + risp);
				// ora ho la stringa di risposta la posso tokenizzare per capire se l'utente ha
				// accettato o meno la sfida
				String[] risposta = risp.split(" ");

				// caso di sfida accettata
				if (risposta[1].equals("accettata")) {
					// preparo una stringa da inviare all'utente per notificargli che la sfida è
					// stata accettata e contenente i parametri della sfida
					risp = risp + " dovrai tradurre 8 parole in 60 secondi .";
					// invio al client che aveva richiesto la sfida una stringa di esito negativo
					ByteBuffer bRisp = ByteBuffer.allocateDirect(512);
					// inserisco la stringa nel Bytebuffer
					bRisp = ByteBuffer.wrap(risp.getBytes("UTF-8"));
					//provo a comunicare la sfida accetta allo sfidante
					try {
						// vado a scrivere sul canale
						while (bRisp.hasRemaining()) {
							c1.write(bRisp);
						}
					}
					//se lo sfidante è crollato avrò un'eccezione, la prendo qui cosi continuo i comandi sotto. A fine sfida verrà liberato anche 
					//lo sfidante
					catch(Exception e) {
						
					}
					// inizia la sfida tolgo la chiave dell'utente sfidato per rimetterla in
					// op_write dopo
					SelectionKey k2 = c2.keyFor(selector);
					k2.interestOps(0);
				}

				else {
					// mi ha risposto sfida rifiutata

					risp = risp + " .";
					
					// invio al client che aveva richiesto la sfida una stringa di esito negativo
					ByteBuffer bRisp = ByteBuffer.allocateDirect(512);
					// inserisco la stringa nel Bytebuffer
					bRisp = ByteBuffer.wrap(risp.getBytes("UTF-8"));
					
					//provo a comunicare la sfida rifiutata allo sfidante
					try {
						// vado a scrivere sul canale
						while (bRisp.hasRemaining()) {
							c1.write(bRisp);
						}
					}
					//se lo sfidante è crollato avrò un'eccezione, la prendo qui cosi continuo i comandi sotto per liberare gli utenti
					catch(Exception e) {
						
					}

					// rimetto i due utenti a libero perchè la sfida è stat rifiutata e quindi sono
					// di nuovo disponibili per ricevere nuove richieste di sfida
					server.libero(us1);
					server.libero(us2);


					// rimetto sulla read il selettore del main
					SelectionKey key = c1.keyFor(selectorM);
					SelectionKey key2 = c2.keyFor(selectorM);
					key.interestOps(SelectionKey.OP_READ);
					key2.interestOps(SelectionKey.OP_READ);
					this.selectorM.wakeup();// risveglio il selettore

					return; // esco dal thread perchè la sfida non deve inizare
				}
			}
		}
		catch(Exception e){
						
			//se  ho un eccezione controllo se lo sfidante è sempre connesso, in tal caso vuol dire che è crollato lo sfidato prima di rispondere 
			//positivamente o negativamente, invio quindi allo sfidante un messaggio di sfida non accettata 
			if ( (c1.isConnected()))  {
				try {
					// preparo la stringa che notifica allo sfidante che la sfida non è stata
					// accettata
					String tScaduto = "Sfida non accettata .";

					// invio al client che aveva richiesto la sfida una stringa di esito negativo
					ByteBuffer buftimeout = ByteBuffer.allocateDirect(tScaduto.length() + 1);
					// inserisco la stringa nel Bytebuffer
					buftimeout = ByteBuffer.wrap(tScaduto.getBytes("UTF-8"));
					// vado a scrivere sul canale
					while (buftimeout.hasRemaining()) {
						c1.write(buftimeout);
					}

					// rimetto i due utenti a libero perchè la sfida non è stata accettata e sono
					// disponibili per ricevere nuove richieste di sfida
					server.libero(us1);
					server.libero(us2);


					// rimetto sulla read il selettore del main ai due utenti
					SelectionKey key = c1.keyFor(this.selectorM);
					SelectionKey key2 = c2.keyFor(this.selectorM);
					key.interestOps(SelectionKey.OP_READ);
					key2.interestOps(SelectionKey.OP_READ);
					this.selectorM.wakeup();// risveglio il selettore

					return;// esco dal thread perchè la sfida non deve inizare
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
		
		try {

			// ------------------------- FINE GESTIONE RISPOSTA DELLO SFIDATO DA RICEVERE IN TCP E INOLTRO DELLA RISPOSTA ALL'UTENTE SFIDANTE---------------------------

			// se sono a questo punto significa che la sfida può iniziare
			// registro la chiave di c1 sul selettore e metto c2 in scrittura
			this.c1.register(selector, SelectionKey.OP_WRITE);
			this.c2.keyFor(selector).interestOps(SelectionKey.OP_WRITE);

			// variabili booleane che uso per uscire dal ciclo di invio delle parole quando
			// entrambe sono settate a true
			// si setta a true quando l'utente corrispondente ha finito le parole da
			// tradurre
			boolean finito1 = false;
			boolean finito2 = false;

			// ciclo che termina se entrambi gli utenti terminano le parole da tradurre
			while (!finito1 || !finito2) {

				selector.selectedKeys().clear();

				// seleziono i canali pronti, mi blocco se nessuno è pronto
				selector.select();

				// vado ad esaminare le chiavi dei canali pronti attraverso il metodo iterator
				Set<SelectionKey> ReadyKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = ReadyKeys.iterator();

				while (iterator.hasNext()) {

					SelectionKey key = iterator.next();
					
					// elimino la chiave dall'iteratore
					iterator.remove();
					try {
						// è pronta la scrittura
						if (key.isWritable()) {
	
							// recupero il canale
							SocketChannel channel = (SocketChannel) key.channel();
	
							String mess = "";// variabile in cui salvo la parola da inviare ai client
	
						/*   analizzo quale delle due chiavi è pronta e invio la parola da tradurre
							 prelevandola dall'arraylist con le parole pronte
							 aggiungo il punto alla parola per fare in modo che lato client si legga fino
							 al punto senza ciclare all'infinito
							 incremento il numero di parole inviate, mi fermo quando sono arrivato a k
							 tutto questo relativamente all'utente la cui chiave è pronta
						*/
							if (channel.equals(c1)) {
								mess = parole1.get(nParole1).getParolaITA() + " .";
								nParole1++;
							}
							if (channel.equals(c2)) {
	
								mess = parole2.get(nParole2).getParolaITA() + " .";
								nParole2++;
							}
	
							// alloco un buffer della lunghezza di 128 byte per inviare la parola
							ByteBuffer bufParola = ByteBuffer.allocateDirect(128);
	
							// inserisco la stringa nel Bytebuffer
							bufParola = ByteBuffer.wrap(mess.getBytes("UTF-8"));
	
							// vado a scrivere sul canale
							while (bufParola.hasRemaining()) {
								channel.write(bufParola);
							}
	
							// seleziono l'operazione di interesse sulla READ
							key.interestOps(SelectionKey.OP_READ);
	
						}
	
						// è pronta la lettura
						else if (key.isReadable()) {
	
							// recupero il canale
							SocketChannel channel = (SocketChannel) key.channel();
	
							// bytebuffer dove salvare la parola letta, ovvero la traduzione inviata
							ByteBuffer bufParola = ByteBuffer.allocateDirect(512);
	
							String risp = "";
	
							// leggo finchè non trovo il punto
							Boolean fine = false;
	
							while (!fine) {
	
								// azzero il ByteBuffer in caso di più letture
								bufParola.clear();
	
								// leggo dal canale
								int byteLetti = channel.read(bufParola);
	
								//System.out.println("byteletti: "+ byteLetti);
								//caso in cui non legga niente quindi in cui il client abbia fatto cadere la connessione
								if (byteLetti == -1) {
									System.out.println("un client si è disconnesso");
									//guardo quale dei due canali si è disconesso e quindi quale utente
									
									if (channel.equals(c1)) {
										// mi salvo che il client 1 si è disconnesso e che quindi il server deve smettere di inviargli parole
										// chiudo il canale
										c1.close();
										server.removeLoggati(us1); //rimuovo l'utente dalla hashmap degli utenti loggati
										finito1 = true; //setto la variabile della fine della sfida per l'utente 1 in modo da uscire dal ciclo appena ha finito l'utente 2
										this.disconnesso1 = true; //setto a true la variabile che controlla se l'utente 1 si è disconnesso
										
										
									} 
									
									else {
										// mi salvo che il client 2 si è disconnesso e che quindi il server deve smettere di inviargli parole
										// chiudo il canale
										c2.close();
										server.removeLoggati(us2);//rimuovo l'utente dalla hashmap degli utenti loggati
										finito2 = true; //setto la variabile della fine della sfida per l'utente 2 in modo da uscire dal ciclo appena ha finito l'utente 1
										this.disconnesso2 = true;//setto a true la variabile che controlla se l'utente 2 si è disconnesso
										
									}
									
									key.cancel();// cancello la chiave
									// setto la variabile uscita per non procedere con le operazioni che avrebbe continuato a svolgere il server per far continuare la sfida
									this.uscita = true;
									fine = true;// esco dal ciclo while della lettura
									continue;
								}
	
								// ritorno in position 0
								bufParola.flip();
	
								// decodifco il messaggio e lo trasformo in stringa
								CharBuffer BufferChar = StandardCharsets.UTF_8.decode(bufParola);
								risp = risp + BufferChar.toString();
	
								// controllo se ho letto fino al punto
								if (risp.endsWith(".")) {
									fine = true;
								}
							}
							
							// se non ci sono stati errori continuo con la sfida
							if (this.uscita == false) {
								
								risp = risp.substring(0, risp.length() - 1);// tolgo il punto alla parola letta prima di
																			// inserirla nell'arraylist per non fare errori
																			// nel confronto delle traduzioni
	
								// controlla se un client ha terminato perche' ha finito il tempo della sfida,
								// in questo caso è il client a inviare un messaggio tcp di notifica di tempo
								// scaduto
								if (risp.equals("Tempo scaduto ")) {
	
									key.cancel();// cancello la chiave perchè il tempo è scaduto
									
									// guardo quale delle due chiavi mi ha inviato il messaggio e setto la variabile
									// finito relativa a quell'utente a true
									if (channel.equals(c1)) {
										finito1 = true;
									} else {
										finito2 = true;
									}
								}
	
								else {
									// caso in cui il messaggio inviato dall'utente non sia un messaggio di tempo
									// scaduto, quindi che sia una parola tradotta
										
									risp = stdString(risp); // standardizzo la stringa per non fare errori nel confronto
															// della traduzione
	
									// guardo quale delle due chiavi è pronta per capire quale dei due utenti mi ha
									// inviato quella traduzione, per salvarla nell'arraylist corretto
	
									if (channel.equals(c1)) {// utente1
	
										// metto la parola tradotta dall'utente nell'arraylist
										parole1.get(nParole1 - 1).setParolaENG(risp);
	
										// se ho finito le parole da inviare e ho ricevuto le rispettive traduzioni
										// cancello la chiave e setto finito a true per quell'utente
										if (nParole1 == k) {
											key.cancel();
											finito1 = true;
										} else {
											// altrimenti significa che non ho finito le parole quindi seleziono
											// l'operazione di interesse sulla WRITE
											key.interestOps(SelectionKey.OP_WRITE);
	
										}
									}
	
									else {// utente2
	
										// metto la parola tradotta dall'utente nell'arraylist
										parole2.get(nParole2 - 1).setParolaENG(risp);
	
										// se ho finito le parole da inviare e ho ricevuto le rispettive traduzioni
										// cancello la chiave e setto finito a true per quell'utente
										if (nParole2 == k) {
											key.cancel();
											finito2 = true;
										} else {
											// altrimenti significa che non ho finito le parole quindi seleziono
											// l'operazione di interesse sulla WRITE
											key.interestOps(SelectionKey.OP_WRITE);
	
										}
									}
	
								}
							}
	
							//caso in cui invece ci siano stati errori, non faccio nessuna delle operazioni sopra, ma devo resettare la variabile uscita a falase
							else {
								System.out.println("resetto la variabile");
								this.uscita = false;
							}
						}
					}
					
				catch(Exception e) {
					
					System.out.println("un client si è disconnesso");
					//guardo quale dei due canali si è disconesso e quindi quale utente
					

					SocketChannel channel = (SocketChannel) key.channel();
					
					if (channel.equals(c1)) {
						// mi salvo che il client 1 si è disconnesso e che quindi il server deve smettere di inviargli parole
						// chiudo il canale
						c1.close();
						server.removeLoggati(us1);//rimuovo l'utente dalla hashmap degli utenti loggati
						finito1 = true; //setto la variabile della fine della sfida per l'utente 1 in modo da uscire dal ciclo appena ha finito l'utente 2
						this.disconnesso1 = true; //setto a true la variabile che controlla se l'utente 1 si è disconnesso
						
						
					} 
					
					else {
						// mi salvo che il client 2 si è disconnesso e che quindi il server deve smettere di inviargli parole
						// chiudo il canale
						c2.close();
						server.removeLoggati(us2);//rimuovo l'utente dalla hashmap degli utenti loggati
						finito2 = true; //setto la variabile della fine della sfida per l'utente 2 in modo da uscire dal ciclo appena ha finito l'utente 1
						this.disconnesso2 = true;//setto a true la variabile che controlla se l'utente 2 si è disconnesso
						
					}
					
					key.cancel();// cancello la chiave
					
				}

				} // fine iteratore

			} // fine while sfida
		} // fine try

		
		catch (IOException e) {
			

		
				/*try {
					this.c1.close();

					this.c2.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				this.UtentiLoggati.remove(this.us1);
				this.UtentiLoggati.remove(this.us2);*/
		}


		// ------------------------------------------------ASSEGNO I PUNTEGGI E CONTROLLO LE TRADUZIONI----------------------------------------

		// controllo se gli utenti sono già presenti nei file dei punteggi (se hanno mai giocato una sfida)
		// altrimenti li inserisco nel JSONObject dei punteggi con punteggio 0

		if (Punteggi.containsKey(us1) == false) {
			JSONObject us = new JSONObject();
			Punteggi.put(us1, 0);
		}

		if (Punteggi.containsKey(us2) == false) {
			JSONObject us = new JSONObject();
			Punteggi.put(us2, 0);
		}

		// CALCOLI DEI PUNTEGGI RELATIVAMENTE ALLA SFIDA ATTUALE

		// variabili dove memorizzo i punti totalizzati nella sfida attuale dagli utenti
		int punti1 = 0;
		int punti2 = 0;

		// stringhe che uso per inviare l'esito della sfida ai due utenti
		String esito1 = "";
		String esito2 = "";

		// se si è disconnesso l'utente 1 setto tutte le sue parole a vuoto perchè non
		// mi interessa di calcolare i suoi punteggi, la sfida non è più valida
		if (this.disconnesso1 == true) {
			int j;
			for (j = 0; j < parole1.size(); j++) {
				parole1.get(j).setParolaITA("");
			}
		}

		// se si è disconnesso l'utente 2 setto tutte le sue parole a vuoto perchè non
		// mi interessa di calcolare i suoi punteggi, la sfida non è più valida
		if (this.disconnesso2 == true) {
			int k;
			for (k = 0; k < parole2.size(); k++) {
				parole2.get(k).setParolaITA("");
			}
		}

		// inizializzo un vettore dove salvarmi le parole in inglese con le traduzioni
		// corrette del server, per mandare una sola volta richieste al sito
		String vettoreIngl[] = new String[parole1.size()];

		try {
			vettoreIngl = vettoreTrad(); // chiamo la funzione che salvi le traduzioni
		} catch (IOException | ParseException e3) {
			e3.printStackTrace();
		}

		// costruisco le stringhe di esito da inviare agli utenti chiamando la funzione
		// punteggi che calcola il totale, le risposte giuste
		// le risposte sbagliate, le risposte non date e costruisce una stringa di
		// risultati per l'utente
		// tutto questo verificando che non si siano disconnessi

		if (this.disconnesso1 == false) {
			try {
				esito1 = punteggi(parole1, vettoreIngl);
			} catch (IOException | ParseException e2) {
				e2.printStackTrace();
			}
			String[] tok1 = esito1.split(" ");// tokenizzo la stringa perchè so che nella posizione 17 dell'array delle
												// stringhe tokenizzate c'è il punteggio totale ottenuto dall'utente
			// prendo l'intero del totale dall'array che contiene la stringa tokenizzata
			punti1 = Integer.parseInt(tok1[17]);
		} else {
			punti1 = Integer.MIN_VALUE;
			esito1 = " .";
		}

		if (this.disconnesso2 == false) {
			try {
				esito2 = punteggi(parole2, vettoreIngl);
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
			String[] tok2 = esito2.split(" ");// tokenizzo la stringa perchè so che nella posizione 17 dell'array delle
												// stringhe tokenizzate c'è il punteggio totale ottenuto dall'utente
			// prendo l'intero del totale dall'array che contiene la stringa tokenizzata
			punti2 = Integer.parseInt(tok2[17]);
		} else {
			punti2 = Integer.MIN_VALUE;
			esito2=" .";
		}

		// CONFRONTO PER COMUNICARE AGLI UTENTI CHI DEI DUE HA VINTO, CHI HA PERSO O SE
		// HANNO PAREGGIATO

		// caso in cui abbia vinto l'utente 1 (sfidante)
		if (punti1 > punti2) {
			punti1 = punti1 + 3; // assegno a lui 3 punti extra
			esito1 = esito1.substring(0, esito1.length() - 1); // tolgo il punto dalla stringa di esito che aveva
																// preconfezionato la funzione punteggi
			esito1 = esito1 + "hai vinto, 3 punti extra ."; // aggiungo alla stringa di esito dell'utente vincitore
															// un'altra stringa che gli notifica la vittoria

			esito2 = esito2.substring(0, esito2.length() - 1); // tolgo il punto dalla stringa di esito che aveva
																// preconfezionato la funzione punteggi
			esito2 = esito2 + "hai perso .";// aggiungo alla stringa di esito dell'utente che ha perso un'altra stringa
											// che gli notifica che non ha vinto
		}

		// caso in cui abbia vinto l'utente 2 (sfidato)
		else if (punti2 > punti1) {
			punti2 = punti2 + 3;// assegno a lui 3 punti extra
			esito2 = esito2.substring(0, esito2.length() - 1);// tolgo il punto dalla stringa di esito che aveva
																// preconfezionato la funzione punteggi
			esito2 = esito2 + "hai vinto, 3 punti extra .";// aggiungo alla stringa di esito dell'utente vincitore
															// un'altra stringa che gli notifica la vittoria

			esito1 = esito1.substring(0, esito1.length() - 1);// tolgo il punto dalla stringa di esito che aveva
																// preconfezionato la funzione punteggi
			esito1 = esito1 + "hai perso .";// aggiungo alla stringa di esito dell'utente che ha perso un'altra stringa
											// che gli notifica che non ha vinto
		}

		// caso in cui abbiano pareggiato
		else if (punti1 == punti2) {
			esito1 = esito1.substring(0, esito1.length() - 1);// tolgo il punto dalla stringa di esito che aveva
																// preconfezionato la funzione punteggi
			esito2 = esito2.substring(0, esito2.length() - 1);// tolgo il punto dalla stringa di esito che aveva
																// preconfezionato la funzione punteggi

			// aggiungo alla stringa di esito di entrambi gli utenti un'altra stringa per
			// notificargli il pareggio
			esito1 = esito1 + "avete pareggiato .";
			esito2 = esito2 + "avete pareggiato .";
		}

		if (this.disconnesso1 == false) {
			// preparo il bytebuffer per inviare l'esito all'utente
			ByteBuffer bufesito1 = ByteBuffer.allocateDirect(512);

			try {
				bufesito1 = ByteBuffer.wrap(esito1.getBytes("UTF-8"));// incapsulo la stringa nel bytebuffer
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			try {
				// scrivo finchè ci sono dati nel buffer
				while (bufesito1.hasRemaining()) {
					c1.write(bufesito1);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (this.disconnesso2 == false) {

			// preparo il bytebuffer per inviare l'esito all'utente
			ByteBuffer bufesito2 = ByteBuffer.allocateDirect(512);
			try {
				bufesito2 = ByteBuffer.wrap(esito2.getBytes("UTF-8"));// incapsulo la stringa nel bytebuffer
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			try {

				// scrivo finchè ci sono dati nel buffer
				while (bufesito2.hasRemaining()) {
					c2.write(bufesito2);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		// risetto i punti dell'eventuale client disconnesso a 0 così il loro punteggio
		// nel file json rimane immutato altrimentri lo avrei qggiornato con
		// integer.minvalue
		if (this.disconnesso1 == true)
			punti1 = 0;
		if (this.disconnesso2 == true)
			punti2 = 0;

		// prendo i punteggi dei due utenti che hanno partecipato alla sfida dal file
		// json dei punteggi per aggiornarli con i punti della sfida appena giocata
		int puntiOLD1 = Integer.parseInt(String.valueOf(Punteggi.get(us1)));
		int puntiOLD2 = Integer.parseInt(String.valueOf(Punteggi.get(us2)));

		// salvo in due variabili (una per utente) i punteggi aggiornati
		int puntiNEW1 = puntiOLD1 + punti1;
		int puntiNEW2 = puntiOLD2 + punti2;

		// inserisco i punteggi aggiornati nel json dei punteggi
		Punteggi.put(us1, puntiNEW1);
		Punteggi.put(us2, puntiNEW2);
		// aggiorno il file riscrivendolo con il json aggiornato
		server.scriviFileJSON(Punteggi, FilePunteggi);

		// rimetto i due utenti a libero perchè la sfida è terminata e sono di nuovo
		// disponibili per nuove richieste di sfida
		server.libero(us1);
		server.libero(us2);

		// rimetto le chiavi dei due utenti sul selettore del main con iteresse per la
		// lettura se non si sono disconnessi
		if (this.disconnesso1 == false) {

			SelectionKey key = c1.keyFor(selectorM);
			key.interestOps(key.OP_READ);
		}
		if (this.disconnesso2 == false) {
			SelectionKey key2 = c2.keyFor(selectorM);

			key2.interestOps(key2.OP_READ);
		}

		this.selectorM.wakeup(); // risveglio il selettore

	}// run
}

