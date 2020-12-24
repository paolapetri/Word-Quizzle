import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import java.util.Set;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class guiLogged {

	private JFrame frame;
    private String user;
	private Client Client;
	private JTextField textFieldAmico;
	private boolean rifiutata; //variabile booleana in settata a true quando un  utente ritorna alla schermata principale perchè la sua richiesta di sfida
							   //è stata rifiutata e oltre al menu gli viene mostrata un stringa di sfida rifiutata
	@SuppressWarnings("rawtypes")
	private DefaultListModel mod; // variabile che utilizzo per pulire la lista

	//metodo costruttore
	@SuppressWarnings("rawtypes")
	public guiLogged(JFrame iniziale, String user, Client Client,boolean rif) {
		this.user = user;
		this.Client = Client;
		this.rifiutata = rif;
		mod  = new DefaultListModel();
		
		try {
			//quando un utente si trova in questa schermata significa che ha fatto il login o che è tornato indietro da una schermata di sfida
			//chiamando questo metodo si verifica se è la prima volta che il client si trova in questa schermata (subito dopo login) o se è tonato indietro
			//dopo qualche altra operazione nella schermata di sfida. Nel caso sia la prima volta questo metodo lancia il thread che sta in ascolto di richieste
			//di sfida, altrimenti non fa niente
			Client.attesaSfida(this);
		
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//pulisco la schermata iniziale e la assegno al frame
		iniziale.getContentPane().removeAll();
		frame = iniziale;
		
		inizialize();

		iniziale.getContentPane().repaint();
	}	
	
	public void inizialize() {
		
		//JFrame del menu
		frame.setResizable(false);
		frame.getContentPane().setBackground(new Color(255, 250, 205));
		frame.getContentPane().setLayout(null);
		frame.setSize(700,600);
	
		//JLabel utilizzata per segnalare all'utente l'esito di alcune operazioni da lui richieste
		JLabel label_op = new JLabel("");//inizializzata a stringa vuota per settare il testo al momento in cui andrà resa visibile all'utente
		label_op.setForeground(new Color(0, 153, 0));
		label_op.setFont(new Font("SansSerif", Font.BOLD, 20));
		label_op.setBounds(385, 167, 281, 95);
		frame.getContentPane().add(label_op);
	
		//JLabel centrale MENU
		JLabel lblMenu = new JLabel("MENU");
		lblMenu.setForeground(new Color(0, 0, 128));
		lblMenu.setFont(new Font("Sitka Heading", Font.BOLD, 30));
		lblMenu.setBounds(282, 10, 118, 47);
		frame.getContentPane().add(lblMenu);
		
		//JLabel utilizzata per dare indicazioni/segnalare esito durante l'operazione di aggiunta amico
		JLabel lblamico = new JLabel("");//inizializzata a vuota per settare il testo al momento in cui andrà resa visibile all'utente
		lblamico.setFont(new Font("Sitka Subheading", Font.BOLD, 22));
		lblamico.setBounds(408, 194, 258, 40);
		frame.getContentPane().add(lblamico);
		
		//TextField dove inserire il nome dell'amico da aggiungere
		textFieldAmico = new JTextField();
		textFieldAmico.setVisible(false); //appare solo quando si chiama la funzione di aggiungi amico
		textFieldAmico.setBounds(408, 266, 221, 42);
		frame.getContentPane().add(textFieldAmico);
		textFieldAmico.setColumns(10);
		
		//JButton che deve essere premuto dall'utente quando ha inserito il nome dell'amico che vuole aggiungere per dare via all'operazione
		JButton btnADD = new JButton("ADD");
		btnADD.setFont(new Font("Sitka Subheading", Font.BOLD, 16));
		btnADD.setBounds(581, 327, 85, 28);
		frame.getContentPane().add(btnADD);
		btnADD.setVisible(false); //appare solo quando si chiama la funzione di aggiungi amico

		//JButton per iniziare una sfida
		JButton btnSfida = new JButton("SFIDA");
		btnSfida.setFont(new Font("Sitka Subheading", Font.BOLD, 26));
		btnSfida.setBounds(41, 68, 315, 58);
		frame.getContentPane().add(btnSfida);
		
		//JList usata per visualizzare la lista amici o la classifica
		@SuppressWarnings("rawtypes")
		JList listaUtil = new JList();
		listaUtil.setLayoutOrientation(JList.VERTICAL);
		listaUtil.setBackground(new Color(173, 216, 230));
		
		//JScrollpane per scorrere la lista
		JScrollPane scrollPane = new JScrollPane(listaUtil); //lo creo e ci inserisco la lista
		scrollPane.setBounds(418, 167, 221, 318);
		frame.getContentPane().add(scrollPane);
		scrollPane.setVisible(false);//lo rendo inizialmente invisibile, apparirà con le richieste di lista amici
		
		//JScrollpane per scorrere la classifica
		JScrollPane scrollClassifica = new JScrollPane();
		scrollClassifica.setBounds(418, 167, 221, 318);
		frame.getContentPane().add(scrollClassifica);
		scrollClassifica.setVisible(false);//lo rendo inizialmente invisibile, apparirà con le richieste di classifica
		
		//JButton per visualizzare la lista amici
		JButton btnListaAmici = new JButton("LISTA AMICI");
		btnListaAmici.setFont(new Font("Sitka Subheading", Font.BOLD, 26));
		btnListaAmici.setBounds(41, 221, 315, 58);
		frame.getContentPane().add(btnListaAmici);
		
		//JButton per aggiungere un nuovo amico
		JButton btnAggiungiAmico = new JButton("AGGIUNGI AMICO");
		btnAggiungiAmico.setFont(new Font("Sitka Subheading", Font.BOLD, 26));
		btnAggiungiAmico.setBounds(41, 144, 315, 58);
		frame.getContentPane().add(btnAggiungiAmico);
		
		//JButton per visualizzare il punteggio
		JButton btnMostraPunteggio = new JButton("MOSTRA PUNTEGGIO");
		btnMostraPunteggio.setFont(new Font("Sitka Subheading", Font.BOLD, 26));
		btnMostraPunteggio.setBounds(41, 297, 315, 58);
		frame.getContentPane().add(btnMostraPunteggio);
		
		//JButton per visualizzare la classifica
		JButton btnMostraClassifica = new JButton("MOSTRA CLASSIFICA");
		btnMostraClassifica.setFont(new Font("Sitka Subheading", Font.BOLD, 26));
		btnMostraClassifica.setBounds(41, 376, 315, 58);
		frame.getContentPane().add(btnMostraClassifica);
		
		//JButton per eseguire il logout
		JButton btnLogout = new JButton("LOGOUT");
		btnLogout.setFont(new Font("Sitka Subheading", Font.BOLD, 15));
		btnLogout.setForeground(Color.RED);
		btnLogout.setBounds(146, 444, 106, 28);
		frame.getContentPane().add(btnLogout);
		
		//JButton da premere per confermare l'amico da sfidare
		JButton btnVAI = new JButton("SFIDA!");
		btnVAI.setFont(new Font("Sitka Subheading", Font.BOLD, 16));
		btnVAI.setBounds(556, 498, 110, 21);
		btnVAI.setVisible(false);
		frame.getContentPane().add(btnVAI);
		
		//se torno in questa finestra con sfida rifiutata (ovvero il costruttore della schermata è stato chiamato con la variabile booleana 
		//che contolla se si torna indietro causa richiesta di sfida rifiutata settata false) visualizzo una stringa di sfida rifiutata
		if(this.rifiutata == true) {
			label_op.setText("SFIDA RIFIUTATA");
			label_op.setVisible(true);
		}
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE SFIDA--------------------------------------------
		btnSfida.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				JSONObject listaAmici;
				String esito = "";
				clear(lblamico, label_op,listaUtil,btnADD,textFieldAmico,scrollPane,scrollClassifica,btnVAI);//"pulisco" la schermata
				
				try {
					//recupero la lista amici in modo da far scegliere a un utente uno dei suoi amici per fare la sfida
					listaAmici = Client.lista_amici(user); 
					if(listaAmici == null) {
						//caso in cui la funzione mi restituisca null, quindi in cui non ho amici
						esito= "Non hai ancora amici";
						label_op.setText(esito);//visualizzo una frase con l'esito, settando visible a true
						label_op.setFont(new Font("Sitka Subheading", Font.BOLD, 12));
						label_op.setVisible(true);
					}
					else {//la lista amici non è vuota
			
						JSONArray amici = (JSONArray) listaAmici.get(user); //recupero dal Jsonobject restituito il jsonarray delle amicizie
						mod.addElement("                      LISTA AMICI   ");
						
						for(int i=0; i<amici.size();i++) {
							//le aggiungo una per volta alla lista
							mod.addElement(amici.get(i));
							
						}
						
						listaUtil.setModel(mod);	
						scrollPane.setVisible(true);//rendo lo scrollpane visibile
						btnVAI.setVisible(true);//rendo il bottone per mandare la richiesta a un amico per iniziare la sfida visibile
					}
					
			}catch (IOException | ParseException e1) {
				e1.printStackTrace();
			}
			}
		});

		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE SFIDA!--------------------------------------------
		btnVAI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String amico = (String) listaUtil.getSelectedValue();
				
				clear(lblamico, label_op,listaUtil,btnADD,textFieldAmico,scrollPane,scrollClassifica,btnVAI);
				@SuppressWarnings("unused")
				guisfida guisfida = new guisfida(Client, user, amico, frame, true);
				
			}
		});

		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE LISTA AMICI--------------------------------------------
		btnListaAmici.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				
				JSONObject listaAmici;
				String esito ="";
				
				clear(lblamico, label_op,listaUtil,btnADD,textFieldAmico,scrollPane,scrollClassifica,btnVAI);//"pulisco" la schermata
				
				try {
					listaAmici = Client.lista_amici(user); 
					if(listaAmici == null) {
						//caso in cui la funzione mi restituisca null, quindi in cui non ho amici
						esito= "Non hai ancora amici";
						label_op.setText(esito);//visualizzo una frase con l'esito, settando visible a true
						label_op.setFont(new Font("Sitka Subheading", Font.BOLD, 12));
						label_op.setVisible(true);
					}
					else {//la lista amici non è vuota
			
						JSONArray amici = (JSONArray) listaAmici.get(user); //recupero dal Jsonobject restituito il jsonarray delle amicizie
						mod.addElement("                      LISTA AMICI   ");
						for(int i=0; i<amici.size();i++) {
							//le aggiungo una per volta alla lista
							mod.addElement(amici.get(i));
							
						}
						listaUtil.setModel(mod);	
						scrollPane.setVisible(true);//rendo lo scrollpane visibile
					}
				} catch (IOException | ParseException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE MOSTRA CLASSIFICA--------------------------------------------
		btnMostraClassifica.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				clear(lblamico, label_op,listaUtil,btnADD,textFieldAmico,scrollPane,scrollClassifica,btnVAI);//"pulisco" la schermata
				JSONObject classifica;
				String esito="";
				try {
					classifica = Client.mostra_classifica(user);
					if(classifica == null) {
						//caso in cui la funzione mi restituisca null, quindi in cui non ho amici
						esito= "Non hai ancora amici";
						label_op.setText(esito);//visualizzo una frase con l'esito, settando visible a true
						label_op.setFont(new Font("Sitka Subheading", Font.BOLD, 12));
						label_op.setVisible(true);
					}
					else {//caso in cui la classifica non sia vuota
						JSONArray temp = (JSONArray) classifica.get(user); //recupero dal Jsonobject restituito il jsonarray con gli utenti e i relativi punteggi
						System.out.println(temp.toJSONString());
						
						//recupero il numero totale di utenti in modo da sapere di quante righe sarà la classifica
						int numbUtenti=temp.size();
						
						//inizializzo una matrice con righe il numero degli utenti e 3 colonne(posizione in classifica, nome utente, punti)
						String[][] ranking=new String[numbUtenti][3];
						
						int i;
						//ciclo in cui inserisco tutti gli elementi nelle righe
						for (i=0;i<numbUtenti;i++) {
							
							JSONObject utente=(JSONObject) temp.get(i);//recupero il jsonobject della posizione i
							
							String pos=String.valueOf(i+1);//lo trasformo in stringa e aumento di 1 perchè la classifica parte da 1
							
							ranking[i][0]=pos;//aggiungo la posizione nella colonna 0
							
							//recupero la chiave ovvero il nome dell'utente
							@SuppressWarnings("unchecked")
							Set<String> key=utente.keySet();
							for(String chiave:key) {
								ranking[i][1]=chiave; //nome
								ranking[i][2]=String.valueOf(utente.get(chiave));//con il nome posso recuperare il punteggio, trasformarlo in stringa e 
																				//metterlo nella colonna corrispondente
							}
						}
						
						String[] columnNames= {"POSIZIONE","NOME","PUNTI"}; //nomi delle colonne
						
						JTable table = new JTable(ranking, columnNames); //JTable dove inserisco la matrice costruita sopra e l'array columnNames
						table.setBackground(new Color(173, 216, 230));
						scrollClassifica.setViewportView(table);//aggiungo la tabella allo scrollpane
						scrollClassifica.setVisible(true);//rendo lo scrollpane visibile
						
					}
				} catch (IOException | ParseException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE AGGIUNGI AMICO--------------------------------------------
		btnAggiungiAmico.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear(lblamico, label_op,listaUtil,btnADD,textFieldAmico,scrollPane,scrollClassifica,btnVAI);//"pulisco" la schermata
				
				//rendo visibili la casella per inserire il nome e un'etichetta che lo indichi
				lblamico.setText("Chi vuoi aggiungere?");
				lblamico.setVisible(true);
				textFieldAmico.setVisible(true);
				btnADD.setVisible(true); //rendo visibile il bottone per dare via all'aggiunta dell'amico
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE ADD--------------------------------------------
		btnADD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String amico = "";
				String esito = "";
				
				try {
					//prendo il nome dell'amico da aggiungere dal TextField
					amico = textFieldAmico.getText();
					textFieldAmico.setText("");
					esito = Client.aggiungi_amico(user, amico); 

					//tolgo dalla schermata la casella e il bottone per visualizzare l'etichetta con l'esito dell'operazione 
					textFieldAmico.setVisible(false);
					btnADD.setVisible(false);
					lblamico.setFont(new Font("Sitka Subheading", Font.BOLD, 12));
					lblamico.setText(esito);
					lblamico.setVisible(true);
					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE MOSTRA PUNTEGGIO--------------------------------------------
		btnMostraPunteggio.addActionListener(new ActionListener() {
			String punteggio = "";
			public void actionPerformed(ActionEvent e) {
				clear(lblamico, label_op,listaUtil,btnADD,textFieldAmico,scrollPane,scrollClassifica,btnVAI);//"pulisco" la schermata
				
				try {
					punteggio = Client.mostra_punteggio(user);
					//rendo visibile una label con il punteggio
					label_op.setText("IL TUO PUNTEGGIO E': " + punteggio);
					label_op.setForeground(Color.ORANGE);
					label_op.setVisible(true);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//OPERAZIONI ESEGUITE QUANDO UN UTENTE PREME IL BOTTONE LOGOUT--------------------------------------------
		btnLogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				try {
					Client.logout(user);
					//torno alla finestra principale passandogli il frame
					@SuppressWarnings("unused")
					guiiniziale principale = new guiiniziale(Client, frame);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
	}
	
	//metodo chiamato dal thread che sta in ascolto di richieste di sfida, in modo da portare l'utente in una schermata dove può accettare o meno la sfida
	public void sfidato(String amico2) {
		@SuppressWarnings("unused")
		guisfida guisfida = new guisfida(Client, user, amico2, frame, false);
	}
	
	//funzione per "pulire" la schermata dai risultati delle operazioni richieste precedentemente o dai text field e label utili alle altre operazioni
	@SuppressWarnings("unchecked")
	private void clear(JLabel lblamico, JLabel label_op, @SuppressWarnings("rawtypes") JList listaUtil, JButton btnADD, JTextField textFieldAmico, 
							JScrollPane scrollPane,JScrollPane scrollClassifica, JButton viasfida) {
			lblamico.setVisible(false);
			label_op.setVisible(false);
			btnADD.setVisible(false);
			scrollPane.setVisible(false);
			textFieldAmico.setVisible(false);
			scrollClassifica.setVisible(false);
			viasfida.setVisible(false);
			mod.clear();
			listaUtil.setModel(mod);
			
	}
	}