import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class guisfida{

	private JFrame frame; //frame che prenderà la schermata precedente
	private Client client; //istanza del client
	private String utente; //nome utente del client
	private String amico; //nome amico da sfidare
	
	//label e text field per la schermata di sfida
	private JTextField textFieldTRAD;
	private JLabel lblParolaDaTradurre;
	private JLabel parolaITA;
	private JLabel lblTraduzione; 
	private JLabel lblInAttesaDi;
	private JLabel lblSfida;
	private JLabel labelRIS;
	private JLabel lblNUMPAROLA;
	
	//bottoni per la schermata di sfida
	private JButton btnOK;
	private JButton btnSkip;
	private JButton btnAccetta;
	private JButton btnRifiuta;
	private JButton btnTornaAlMenu;
	
	//variabile in cui salvo il numero delle parole inviate fino ad un certo momento, per rendere il contatore visibile al client
	private int nParole;
	
	//timer e timer task per realizzare il timeout di durata della sfida e timeout di risposta della richiesta di sfida
	private Timer timer;
	private TimerTask task;
	private Timer timer2;
	private TimerTask task2;
	
	private boolean sfidante;//variabile settata a true quando questa finestra si deve aprire in modalità sfidante(ovvero in attesa della risposta)
							//settata a false quando si deve aprire in modalità sfidato ovvero con la schermata di accetta/rifiuta della richiesta di sfida
	
	//costruttore
	public guisfida(Client client, String utente, String amico, JFrame oldframe, boolean sfidante) {
	
		this.nParole=0; //inizializzo a 0 il numero delle parole
		this.client = client;
		this.utente = utente;
		this.amico = amico;
		this.frame=oldframe; //assegno al frame il vecchio frame della schermata principale
		this.sfidante = sfidante;
		
		//passo il frame della schermata principale e lo resetto per avviare la schermata di sfida
		frame.getContentPane().removeAll();	
		frame.repaint();
		frame.setVisible(true);
		initialize();
		
		//se è lo sfidante allora lancio il thread che si occupa di chiamare la funzione sfida della classe Client e inviare la richiesta di sfida
		if(this.sfidante == true) {
			execSfida sfida = new execSfida();
			Thread t = new Thread(sfida);
			t.start();
		}
		
		
	}

	//metodo che uso per pulire la schermata dopo la sfida
	private void clean() {
		
		textFieldTRAD.setVisible(false);
		lblTraduzione.setVisible(false);
		lblNUMPAROLA.setVisible(false);
		lblParolaDaTradurre.setVisible(false);
		btnOK.setVisible(false);
		btnSkip.setVisible(false);
		lblSfida.setVisible(false);
		parolaITA.setVisible(false);
		
	}
	
	//classe che contiene il task del thread lanciato se l'utente è lo sfidante che manda la richiesta di sfida e riceve l'esito della richiesta
	//il metodo Sfida poi chiamerà a sua volta un metodo che cambia la schermata in caso di risposta positiva oppure torna alla schermata principale
	private class execSfida implements Runnable{

		public void run() {
			try {
				client.Sfida(utente, amico, guisfida.this);
				
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void initialize() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setSize(700,600);
		
		//JLabel centrale visualizzata nella schermata durante la sfida
		lblSfida = new JLabel("SFIDA!");
		lblSfida.setForeground(Color.RED);
		lblSfida.setFont(new Font("Sitka Heading", Font.BOLD | Font.ITALIC, 46));
		lblSfida.setBounds(244, 10, 188, 73);
		lblSfida.setVisible(false);
		frame.getContentPane().add(lblSfida);
		
		//JLabel in attesa di risposta del client
		lblInAttesaDi = new JLabel("In attesa di risposta..");
		lblInAttesaDi.setFont(new Font("Sitka Subheading", Font.BOLD, 22));
		lblInAttesaDi.setBounds(187, 21, 278, 32);
		lblInAttesaDi.setVisible(false);
		frame.getContentPane().add(lblInAttesaDi);
		
		//JLabel che viene visualizzata allo sfidato per chiedergli se vuole accettare la sfida e da quale utente arriva la richiesta
		JLabel labelaccett = new JLabel("");
		labelaccett.setFont(new Font("Sitka Subheading", Font.BOLD, 20));
		labelaccett.setBounds(187, 144, 361, 62);
		labelaccett.setVisible(false);
		frame.getContentPane().add(labelaccett);
		
		//JButton che l'utente può premere in caso voglia accettare la sfida
		btnAccetta = new JButton("ACCETTA");
		btnAccetta.setForeground(new Color(0, 100, 0));
		btnAccetta.setFont(new Font("Sitka Subheading", Font.BOLD, 15));
		btnAccetta.setBounds(60, 279, 161, 52);
		btnAccetta.setVisible(false);
		frame.getContentPane().add(btnAccetta);
		
		//JButton che l'utente può premere in caso voglia riifutare la sfida
		btnRifiuta = new JButton("RIFIUTA");
		btnRifiuta.setForeground(new Color(178, 34, 34));
		btnRifiuta.setFont(new Font("Sitka Subheading", Font.BOLD, 15));
		btnRifiuta.setBounds(431, 279, 161, 52);
		btnRifiuta.setVisible(false);
		frame.getContentPane().add(btnRifiuta);
		
		//distinguo i due casi sfidato e sfidante
		
		//se il client è lo sfidante, la finestra si apre in attesa della risposta dell'utente sfidato
		if(sfidante == true) {
			lblInAttesaDi.setVisible(true);
		}
		
		//se il client è lo sfidato, la finestra si apre con un messaggio di richiesta di sfida da accettare o meno con i due bottoni accetta e rifiuta
		else {
			labelaccett.setText("Accetti la sfida da "+amico + " ?");
			labelaccett.setVisible(true);
			btnAccetta.setVisible(true);
			btnRifiuta.setVisible(true);
			
			//inizializzo un timer per fare in modo che lo sfidato abbia un tempo limitato per rispondere al messaggio di richiesta di sfida 
			timer2 = new Timer();
			
			//task che viene eseguito in caso di scadenza del timer
			task2 = new TimerTask() {

				public void run() {
					//se scade il timer l'utente viene riportato alla schermata principale perchè non ha fatto in tempo a rispondere
					@SuppressWarnings("unused")
					guiLogged guiLogged = new guiLogged(frame,utente,client,false);
				}
				
			};
			
			//setto il timer a 10 secondi dopo il quale verrà eseguito il task2
			timer2.schedule(task2, 10000);
			
		}
		
		//JLabel usata per indicare all'utente la parola italiana da tradurre in inglese
		lblParolaDaTradurre = new JLabel("PAROLA DA TRADURRE:");
		lblParolaDaTradurre.setFont(new Font("Sitka Subheading", Font.BOLD | Font.ITALIC, 18));
		lblParolaDaTradurre.setBounds(10, 123, 240, 32);
		lblParolaDaTradurre.setVisible(false);
		frame.getContentPane().add(lblParolaDaTradurre);
		
		//JLabel nella quale verrà settata ogni volta la parola italiana da tradurre
		parolaITA = new JLabel("");
		parolaITA.setForeground(Color.BLUE);
		parolaITA.setFont(new Font("Sitka Subheading", Font.BOLD, 20));
		parolaITA.setBounds(245, 104, 284, 66);
		parolaITA.setVisible(false);
		frame.getContentPane().add(parolaITA);
		
		//JLabel usata per indicare all'utente dove dovrà inserire la traduzione della parola
		lblTraduzione = new JLabel("TRADUZIONE:");
		lblTraduzione.setFont(new Font("Sitka Subheading", Font.BOLD | Font.ITALIC, 18));
		lblTraduzione.setBounds(10, 196, 240, 32);
		lblTraduzione.setVisible(false);
		frame.getContentPane().add(lblTraduzione);
		
		//JTextField dove l'utente dovrà inserire la traduzione della parola
		textFieldTRAD = new JTextField();
		textFieldTRAD.setBounds(163, 196, 353, 32);
		textFieldTRAD.setVisible(false);
		frame.getContentPane().add(textFieldTRAD);
		textFieldTRAD.setColumns(10);
		
		//JButton che l'utente dovrà premere per passare alla parola successiva da tradurre dopo averla inserita
		btnOK = new JButton("NEXT");
		btnOK.setFont(new Font("Sitka Text", Font.BOLD, 14));
		btnOK.setBounds(469, 252, 85, 27);
		btnOK.setVisible(false);
		frame.getContentPane().add(btnOK);
		
		//JButton che l'utente dovrà premere per passare alla parola successiva da tradurre senza inviare nessuna traduzione
		//(il server la valuterà come risposta non data)
		btnSkip = new JButton("SKIP");
		btnSkip.setFont(new Font("Sitka Text", Font.BOLD, 14));
		btnSkip.setBounds(148, 252, 85, 27);
		btnSkip.setVisible(false);
		frame.getContentPane().add(btnSkip);
		
		//JLabel utilizzata per comunicare all'utente i risultati della sfida
		labelRIS = new JLabel("");
		labelRIS.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		labelRIS.setBounds(89, 123, 503, 146);
		labelRIS.setVisible(false);
		frame.getContentPane().add(labelRIS);
		
		//JLabel utilizzata per far vedere al client quante parole ha tradotto fino ad ora rispetto al totale delle parole da tradurre
		lblNUMPAROLA = new JLabel("");
		lblNUMPAROLA.setFont(new Font("Sitka Subheading", Font.BOLD, 27));
		lblNUMPAROLA.setBounds(490, 144, 77, 42);
		lblNUMPAROLA.setVisible(false);
		frame.getContentPane().add(lblNUMPAROLA);
		
		//JButton inserito nella schermata dove vengono visualizzati i risultati della sfida per tornare indietro
		btnTornaAlMenu = new JButton("TORNA AL MENU");
		btnTornaAlMenu.setFont(new Font("Sitka Subheading", Font.BOLD, 16));
		btnTornaAlMenu.setBounds(226, 337, 228, 42);
		btnTornaAlMenu.setVisible(false);
		frame.getContentPane().add(btnTornaAlMenu);

		//OPERAZIONI ESEGUITE QUANDO SI ACCETTA UNA RICHIESTA DI SFIDA PREMENDO IL BOTTONE ACCETTA
		btnAccetta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String risp = "Sfida accettata .";//stringa da inviare al server per comunicare che si è accettata la sfida
				timer2.cancel();//cancello il timer perchè ho risposto entro i 10 secondi e non deve eseguire più il task
				timer2.purge();
				
				try {
					client.scriviMess(risp);//scrivo il messaggio al server
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				//pulisco la schermata di accettazione della sfida
				labelaccett.setVisible(false);
				btnAccetta.setVisible(false);
				btnRifiuta.setVisible(false);
				
				//cambio la finestra con quella della sfida vera e propria
				cambiaFinestrasfidato();
				
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO SI RIFIUTA UNA RICHIESTA DI SFIDA PREMENDO IL BOTTONE RIFIUTA
		btnRifiuta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			
				String risp = "Sfida rifiutata .";//stringa da inviare al server per comunicare che si è accettata la sfida
				timer2.cancel();//cancello il timer perchè ho risposto entro i 10 secondi e non deve eseguire più il task
				timer2.purge();
				try {
					client.scriviMess(risp);//scrivo il messaggio al server
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				//poichè ho rifiutato la sfida, devo tornare alla schermata principale
				@SuppressWarnings("unused")
				guiLogged guilogged = new guiLogged(frame,utente,client,false); 
			}
		});
	}
	
	//metodo invocato quando un utente sfidato accetta la sfida e la schermata viene settata in modo da fargli rispondere alle parole da tradurre
	public void cambiaFinestrasfidato() {
		frame.setSize(600, 450);
		
		//inizializzo un timer in modo da far durare la sfida per un tempo limitato
		timer =new Timer();
		
		//task che viene eseguito in caso di scadenza del timer
		task = new TimerTask() {
			public void run() {
				try {
					
					client.scriviMess("Tempo scaduto .");//scrivo al server un messaggio di tempo scaduto in modo che 
														// non invii più le parole da tradurre ma si prepari ad inviare i risultati della sfida
					clean();//pulisco la schermata per visualizzare i risultati
					
					String ris="";//stringa in cui salvo il messaggio ricevuto dal server con i risultati
					ris = client.leggiMess();//leggo i risultati inviati dal server
					ris = ris.substring(0,ris.length()-1); //tolgo il punto dalla stringa
					ris ="<html>" + ris +"</html>"; //faccio in modo che la stringa entri nella schermata andando più volte a capo
					labelRIS.setText(ris); //setto la stringa alla JLabel
					
					//rendo visibili i risultati e il bottone per tornare al menu
					labelRIS.setVisible(true);
					btnTornaAlMenu.setVisible(true);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		};
		
		//setto il timer a 60 secondi dopo il quale verrà eseguito il task
		timer.schedule(task, 60000);
		
		//preparo la schermata della sfida
		lblInAttesaDi.setVisible(false);
		textFieldTRAD.setVisible(true);
		lblTraduzione.setVisible(true);
		lblParolaDaTradurre.setVisible(true);
		btnOK.setVisible(true);
		btnSkip.setVisible(true);
		lblSfida.setVisible(true);
		
		String parola="";//stringa in cui salvo la parola italiana da tradurre inviata dal server 
		try {
			parola = client.leggiMess(); //leggo dal server la parola da tradurre
		} catch (IOException e) {
			e.printStackTrace();
		}
		parola = parola.substring(0, parola.length()-1); //tolgo il punto alla parola
		
		//setto la parola alla JLabel e la rendo visibile
		parolaITA.setText(parola);
		parolaITA.setVisible(true);
		
		//incremento la variabile delle parole perchè parto da 0 e la trasformo in stringa
		String numero = String.valueOf(nParole+1);
		//setto la JLabel con la stringa del numero della parola corrente e la rendo visibile
		lblNUMPAROLA.setText(numero+"/8");
		lblNUMPAROLA.setVisible(true);
		
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE NEXT PER PASSARE ALLA PAROLA SUCCESSIVA
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String parolaTradotta = "";//stringa in cui salvo la parola tradotta presa dal JTextField
				String ris="";//stringa in cui salvo gli esiti della sfida inviati dal server
				parolaTradotta= textFieldTRAD.getText(); //prendo dal JTextField la parola tradotta inserita dall'utente
				String parolaI=""; //stringa in cui salvo la parola italiana da tradurre inviata dal server 
				
				try {
					nParole++;//incremento ogni volta il numero delle parole
					parolaTradotta = parolaTradotta + " ."; //inserisco il punto per far leggere fino a lì la parola al server
					client.scriviMess(parolaTradotta); //scrivo al server la parola tradotta
					textFieldTRAD.setText(""); //"pulisco" il JTextField
					
					if(nParole == 8) {
						//entro in questo if se l'utente ha tradotto tutte le 8 parole entro un minuto
						
						timer.cancel(); //cancello il timer perchè non deve più eseguire il task dopo un minuto
						timer.purge();
						
						clean();//pulisco la schermata per visualizzare i risultati
						
						ris = client.leggiMess();//leggo dal server la stringa con i risultati
						ris = ris.substring(0,ris.length()-1);//tolgo il punto
						ris ="<html>" + ris +"</html>"; //faccio entrare la stringa completa nella schermata andando più volte a capo
						
						//setto la JLabel con i risultati e il bottone per tornare al menu
						labelRIS.setText(ris);
						labelRIS.setVisible(true);
						btnTornaAlMenu.setVisible(true);
						
					}
					else {
						//caso in cui il tempo non sia ancora scaduto e non abbia finito le parole da tradurrre
						
						parolaI=client.leggiMess();//leggo un'altra parola italiana da tradurre dal server
						parolaI = parolaI.substring(0, parolaI.length()-1); //tolgo il punto
						parolaITA.setText(parolaI); //setto la JLabel con la parola da tradurre
						
						//incremento la variabile delle parole perchè partivo da 0 e la trasformo in stringa
						String numero = String.valueOf(nParole+1);
						//setto la JLabel con la stringa del numero della parola corrente e la rendo visibile
						lblNUMPAROLA.setText(numero+"/8");
						lblNUMPAROLA.setVisible(true);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		

		//AZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE SKIP
		btnSkip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String ris="";//stringa in cui salvo gli esiti della sfida inviati dal server
				String parolaI="";//stringa in cui salvo la parola italiana da tradurre inviata dal server 
				
				try {
					nParole++;//incremento ogni volta il numero delle parole
					client.scriviMess("non_data .");//scrivo al server una stringa per fargli capire che l'utente ha premuto skip e che la risposta deve essere valutata
													//come non data
					textFieldTRAD.setText("");//"pulisco" il JTextField
					
					if(nParole == 8) { 
						//entro in questo if se l'utente ha risposto a tutte le 8 parole entro un minuto
						
						timer.cancel();//cancello il timer perchè non deve più eseguire il task dopo un minuto
						timer.purge();
						
						clean();//pulisco la schermata per visualizzare i risultati
						
						
						ris = client.leggiMess();//leggo dal server la stringa con i risultati
						ris = ris.substring(0,ris.length()-1);//tolgo il punto
						ris ="<html>" + ris +"</html>"; //faccio entrare la stringa completa nella schermata andando più volte a capo
						
						//setto la JLabel con i risultati e il bottone per tornare al menu
						labelRIS.setText(ris);
						labelRIS.setVisible(true);
						btnTornaAlMenu.setVisible(true);
						
					}
					else {
						//caso in cui il tempo non sia ancora scaduto e non abbia finito le parole da tradurrre
						
						parolaI=client.leggiMess();//leggo un'altra parola italiana da tradurre dal server
						parolaI = parolaI.substring(0, parolaI.length()-1); //tolgo il punto
						parolaITA.setText(parolaI); //setto la JLabel con la parola da tradurre
						
						//incremento la variabile delle parole perchè partivo da 0 e la trasformo in stringa
						String numero = String.valueOf(nParole+1);
						//setto la JLabel con la stringa del numero della parola corrente e la rendo visibile
						lblNUMPAROLA.setText(numero+"/8");
						lblNUMPAROLA.setVisible(true);
						
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO UN CLIENT PREME IL BOTTONE TORNA AL MENU DOPO AVER LETTO LA STRINGA DEI RISULTATI
		btnTornaAlMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//risetto la schermata principale del menu
				@SuppressWarnings("unused")
				guiLogged guilogged = new guiLogged(frame,utente,client,false);
			}
		});
		
	}
	
	//metodo invocato quando un utente sfidante riceve dal server l'esito della richiesta di sfida e in base alla risposta verrà settata la schermata
	public void cambiaFinestra(String risposta) {
		
		//tokenizzo la stringa di risposta così da analizzarla 
		String[] rispTOK = risposta.split(" ");
		
		//caso in cui il server non abbia inoltrato la richiesta perchè i parametri non erano validi
		if(risposta.equals("Sfida rifiutata errore sui parametri .")) {
			
			//pulisco la schermata e torno alla schermata precedente del menu dove gli verrà comunicato l'esito di sfida rifiutata
			frame.getContentPane().removeAll();	
			frame.repaint();
			@SuppressWarnings("unused")
			guiLogged guilogged = new guiLogged(frame, utente,client,true);
			
		}
		
		//caso di sfida rifiutata perchè l'utente sfidato ha premuto il bottone rifiuta
		else if(risposta.equals("Sfida rifiutata .")) {
			
			//pulisco la schermata e torno alla schermata precedente del menu dove gli verrà comunicato l'esito di sfida rifiutata
			frame.getContentPane().removeAll();	
			frame.repaint();
			@SuppressWarnings("unused")
			guiLogged guilogged = new guiLogged(frame, utente,client,true);
		}
		
		//caso di sfida rifiutata perchè il server ha trovato l'utente occupato in un'altra sfida e non gli ha potuto inviare la richiesta
		else if(risposta.equals("Sfida rifiutata, utente occupato .")) {

			//pulisco la schermata e torno alla schermata precedente del menu dove gli verrà comunicato l'esito di sfida rifiutata
			frame.getContentPane().removeAll();	
			frame.repaint();
			@SuppressWarnings("unused")
			guiLogged guilogged = new guiLogged(frame, utente,client,true);
		}
		
		//caso di sfida rifiutata perchè l'utente sfidato non ha risposto entro i 10 secondi alla richiesta di sfida
		else if(rispTOK[1].equals("non")) {
			frame.getContentPane().removeAll();	
			frame.repaint();
			@SuppressWarnings("unused")
			guiLogged guilogged = new guiLogged(frame, utente,client,true);
			
		}
		
		//caso di sfida accettata
		else if(rispTOK[1].equals("accettata")) {
			
			frame.setSize(600, 450);
			
			//inizializzo un timer in modo da far durare la sfida per un tempo limitato
			timer =new Timer();
			
			//task che viene eseguito in caso di scadenza del timer
			task = new TimerTask() {
				public void run() {
					try {
						
						client.scriviMess("Tempo scaduto .");//scrivo al server un messaggio di tempo scaduto in modo che 
															// non invii più le parole da tradurre ma si prepari ad inviare i risultati della sfida
						clean();//pulisco la schermata per visualizzare i risultati
						
						String ris="";//stringa in cui salvo il messaggio ricevuto dal server con i risultati
						ris = client.leggiMess();//leggo i risultati inviati dal server
						ris = ris.substring(0,ris.length()-1); //tolgo il punto dalla stringa
						ris ="<html>" + ris +"</html>"; //faccio in modo che la stringa entri nella schermata andando più volte a capo
						labelRIS.setText(ris); //setto la stringa alla JLabel
						
						//rendo visibili i risultati e il bottone per tornare al menu
						labelRIS.setVisible(true);
						btnTornaAlMenu.setVisible(true);
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			};
			
			//setto il timer a 60 secondi dopo il quale verrà eseguito il task
			timer.schedule(task, 60000);
			
			//preparo la schermata della sfida
			lblInAttesaDi.setVisible(false);
			textFieldTRAD.setVisible(true);
			lblTraduzione.setVisible(true);
			lblParolaDaTradurre.setVisible(true);
			btnOK.setVisible(true);
			btnSkip.setVisible(true);
			lblSfida.setVisible(true);
			
			String parola="";//stringa in cui salvo la parola italiana da tradurre inviata dal server 
			try {
				parola = client.leggiMess(); //leggo dal server la parola da tradurre
			} catch (IOException e) {
				e.printStackTrace();
			}
			parola = parola.substring(0, parola.length()-1); //tolgo il punto alla parola
			
			//setto la parola alla JLabel e la rendo visibile
			parolaITA.setText(parola);
			parolaITA.setVisible(true);
			
			//incremento la variabile delle parole perchè parto da 0 e la trasformo in stringa
			String numero = String.valueOf(nParole+1);
			//setto la JLabel con la stringa del numero della parola corrente e la rendo visibile
			lblNUMPAROLA.setText(numero+"/8");
			lblNUMPAROLA.setVisible(true);
			
			
			//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE NEXT PER PASSARE ALLA PAROLA SUCCESSIVA
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					
					String parolaTradotta = "";//stringa in cui salvo la parola tradotta presa dal JTextField
					String ris="";//stringa in cui salvo gli esiti della sfida inviati dal server
					parolaTradotta= textFieldTRAD.getText(); //prendo dal JTextField la parola tradotta inserita dall'utente
					String parolaI=""; //stringa in cui salvo la parola italiana da tradurre inviata dal server 
					
					try {
						nParole++;//incremento ogni volta il numero delle parole
						parolaTradotta = parolaTradotta + " ."; //inserisco il punto per far leggere fino a lì la parola al server
						client.scriviMess(parolaTradotta); //scrivo al server la parola tradotta
						textFieldTRAD.setText(""); //"pulisco" il JTextField
						
						if(nParole == 8) {
							//entro in questo if se l'utente ha tradotto tutte le 8 parole entro un minuto
							
							timer.cancel(); //cancello il timer perchè non deve più eseguire il task dopo un minuto
							timer.purge();
							
							clean();//pulisco la schermata per visualizzare i risultati
							
							ris = client.leggiMess();//leggo dal server la stringa con i risultati
							ris = ris.substring(0,ris.length()-1);//tolgo il punto
							ris ="<html>" + ris +"</html>"; //faccio entrare la stringa completa nella schermata andando più volte a capo
							
							//setto la JLabel con i risultati e il bottone per tornare al menu
							labelRIS.setText(ris);
							labelRIS.setVisible(true);
							btnTornaAlMenu.setVisible(true);
							
						}
						else {
							//caso in cui il tempo non sia ancora scaduto e non abbia finito le parole da tradurrre
							
							parolaI=client.leggiMess();//leggo un'altra parola italiana da tradurre dal server
							parolaI = parolaI.substring(0, parolaI.length()-1); //tolgo il punto
							parolaITA.setText(parolaI); //setto la JLabel con la parola da tradurre
							
							//incremento la variabile delle parole perchè partivo da 0 e la trasformo in stringa
							String numero = String.valueOf(nParole+1);
							//setto la JLabel con la stringa del numero della parola corrente e la rendo visibile
							lblNUMPAROLA.setText(numero+"/8");
							lblNUMPAROLA.setVisible(true);
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			

			//AZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE SKIP
			btnSkip.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					
					String ris="";//stringa in cui salvo gli esiti della sfida inviati dal server
					String parolaI="";//stringa in cui salvo la parola italiana da tradurre inviata dal server 
					
					try {
						nParole++;//incremento ogni volta il numero delle parole
						client.scriviMess("non_data .");//scrivo al server una stringa per fargli capire che l'utente ha premuto skip e che la risposta deve essere valutata
														//come non data
						textFieldTRAD.setText("");//"pulisco" il JTextField
						
						if(nParole == 8) { 
							//entro in questo if se l'utente ha risposto a tutte le 8 parole entro un minuto
							
							timer.cancel();//cancello il timer perchè non deve più eseguire il task dopo un minuto
							timer.purge();
							
							clean();//pulisco la schermata per visualizzare i risultati
							
							
							ris = client.leggiMess();//leggo dal server la stringa con i risultati
							ris = ris.substring(0,ris.length()-1);//tolgo il punto
							ris ="<html>" + ris +"</html>"; //faccio entrare la stringa completa nella schermata andando più volte a capo
							
							//setto la JLabel con i risultati e il bottone per tornare al menu
							labelRIS.setText(ris);
							labelRIS.setVisible(true);
							btnTornaAlMenu.setVisible(true);
							
						}
						else {
							//caso in cui il tempo non sia ancora scaduto e non abbia finito le parole da tradurrre
							
							parolaI=client.leggiMess();//leggo un'altra parola italiana da tradurre dal server
							parolaI = parolaI.substring(0, parolaI.length()-1); //tolgo il punto
							parolaITA.setText(parolaI); //setto la JLabel con la parola da tradurre
							
							//incremento la variabile delle parole perchè partivo da 0 e la trasformo in stringa
							String numero = String.valueOf(nParole+1);
							//setto la JLabel con la stringa del numero della parola corrente e la rendo visibile
							lblNUMPAROLA.setText(numero+"/8");
							lblNUMPAROLA.setVisible(true);
							
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			
			//OPERAZIONI ESEGUITE QUANDO UN CLIENT PREME IL BOTTONE TORNA AL MENU DOPO AVER LETTO LA STRINGA DEI RISULTATI
			btnTornaAlMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//risetto la schermata principale del menu
					@SuppressWarnings("unused")
					guiLogged guilogged = new guiLogged(frame,utente,client,false);
				}
			});
		}
	}

}