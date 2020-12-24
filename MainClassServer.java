import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Set;


public class MainClassServer {

	static Server server = null;
	
	public static void main(String[] args) {
		
		final int serverPort = 23456;  
		Selector selector = null;
		ServerSocketChannel serverSocket = null;
		
		try {
			
			//creo una listening ServersocketChannel non bloccante
			serverSocket = ServerSocketChannel.open();
			serverSocket.socket().bind(new InetSocketAddress("localhost", serverPort));
			serverSocket.configureBlocking(false);
			
			//creo il selettore
			selector = Selector.open();
			
			//registro sul selettore il canale del server dove i client possono collegarsi
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
		}
		
		catch( IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		

		//Rendo disponibile il metodo registra_utente tramite RMI 
		
		final String ServiceName = "registraUtente"; //nome per il riferimento nel registro
		final int portaRMI = 4000;
		try {
			server = new Server(); //inizializzo un'istanza dell'oggetto server
			LocateRegistry.createRegistry(portaRMI); //creo un registro sulla porta PORT
			Registry r=LocateRegistry.getRegistry(portaRMI); //ottengo un riferimento al registro
			r.rebind(ServiceName,server); //inserisco l'oggetto server nel registro con il nome di ServiceName associato
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		while(true){
			
			System.out.println("Selettore in attesa di canali pronti per comunicare");
			try{
				//seleziono i canali pronti, mi blocco se nessuno è pronto
				selector.select();
			}
			catch( IOException e){
				e.printStackTrace();
				System.exit(1);
			}
			
			//vado ad esaminare le chiavi dei canali pronti attraverso il metodo iterator
			Set<SelectionKey> ReadyKeys= selector.selectedKeys();
			Iterator <SelectionKey> iterator=ReadyKeys.iterator();
			
			while(iterator.hasNext()) {

				SelectionKey key=iterator.next();
				
				//elimino la chiave dall'iteratore
				iterator.remove();
				
				try {	
					//il client ha richiesto la connessione
					if (key.isAcceptable()){
						
						//creo la socketChannel non bloccante per comunicare con il client
						ServerSocketChannel channel= (ServerSocketChannel) key.channel();
						SocketChannel client=channel.accept();
						System.out.println("New client "+ client.getRemoteAddress());
						client.configureBlocking(false);
						
						ByteBuffer message = ByteBuffer.allocate(512); // alloco un ByteBuffer per la lettura del messagio del client di dim BUFDIM
						
						client.register(selector, SelectionKey.OP_READ, message); //registro il canale del client con il selettore e imposto la selection 
																//key con interesse per l'operazione di lettura e imposto l'allegato della chiave su message 
					}
					
						
					//è pronta la lettura
					else if (key.isReadable()){

						SocketChannel client = (SocketChannel) key.channel(); //socket channel prende il canale pronto per la lettura
						ByteBuffer message = (ByteBuffer) key.attachment(); //recupero l'oggetto allegato a questa chiave
						
						//leggo dal canale fino a che non trovo " . "
						Boolean fine=false;
						String datiLetti = "";
						while(!fine) {

							//Azzero il ByteBuffer in caso di più letture
							message.clear();
							//leggo dal canale
							client.read(message);
							
							//ritorno in position 0
							message.flip();
								
							//decodifco il messaggio in charBuffer
							CharBuffer BufferChar=StandardCharsets.UTF_8.decode(message);

							//trasformo in stringa il messaggio e lo concateno a StringaDati
							datiLetti = datiLetti + BufferChar.toString();
							
							//se trovo "." come ultimo Char allora termino la lettura
							if(datiLetti.endsWith(".")) {
								fine=true;
								}
						}
					
							//Azzero il ByteBuffer, ci voglio salvare la risposta che verrà inviata al client
							message.clear();
						
							//chiamo il metodo analisiMess che andrà ad eseguire il comando contenuto nella stringa
							String result = "";
							result = server.analisiMess(datiLetti, message, client, selector);
							
							message=ByteBuffer.wrap(result.getBytes("UTF-8"));
							
							 //tokenizzo la stringa restituita dal metodo
							 String[] datiTOK = result.split(" ");
							  
							 //tokenizzo la stringa letta con la richiesta del cliente
							 String[] datilettiTOK = datiLetti.split(" ");
							 
							 if(!datilettiTOK[0].equals("Sfida")) {
								//guardo se la parola ricevuta dal client non era sfida rimetto il selettore sulla write

								 key.interestOps(SelectionKey.OP_WRITE);
							    	key.attach(message);
							 }
							 else {
								 //se la parola era sfida guardo se la sfida può partire altrimenti rimetto la write
								 if(datiTOK[0].equals("Sfida")){
									 //caso in cui la sfida non parta perchè il server rileva errori sui parametri (la funzione analisiMess ritorna la stringa "Sfida rifiutata") 
									 key.interestOps(SelectionKey.OP_WRITE);
								    	key.attach(message);
								 }
							 }

					}
					
					//è pronta la scrittura
					else if (key.isWritable()){

							//recupero il canale
							SocketChannel channel= (SocketChannel) key.channel();
							//recupero l'array di byteArray attaccato
							ByteBuffer msg= (ByteBuffer) key.attachment();
							
							String dati = "";
						
							//scrivo nel canale finchè non termina il ByteBuffer
							while(msg.hasRemaining()) {
								channel.write(msg);
							}

						    msg.flip();
						  					   
						    CharBuffer BufferChar=StandardCharsets.UTF_8.decode(msg);
						    dati=dati + BufferChar.toString();

						    //System.out.println("string: " + dati);
						    //se il valore è 1 chiudo il canale e quindi la connessione TPC tra l'utente ed il server
						    if(dati.equals("Logout eseguito con successo .")) {
						    	//cancello la chiave e chiudo il canale
							    key.cancel();
							    channel.close();
						    }
						    //altrimenti imposto il canale sul selettore in modalità lettura in attesa di altri comandi
						    else {
							    key.interestOps(SelectionKey.OP_READ);
						    }

					   }					
				}
			
				//in caso di eccezione devo eliminare l'utente che ha causato il problema dall'hasMap con gli utenti che hanno fatto il Login
				catch( IOException e){
					SocketChannel channel= (SocketChannel) key.channel();
					
					//elimino l'utente dalla lista degli utenti loggati
					server.logoutEXC(channel);	
					
					key.cancel();//rimuovo la chiave di quel canale 
					try {
						channel.close(); 
					} 
					catch (IOException e1) {
						e1.printStackTrace();
					}
					
				}
			}
		} 
		
	}
}
	
	