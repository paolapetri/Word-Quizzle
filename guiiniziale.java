import javax.swing.JFrame;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.awt.event.ActionEvent;

public class guiiniziale {

	public JFrame frame;
	private JTextField textField;
	private JPasswordField textField_1;
	private Client Client;

	public guiiniziale(Client Client, JFrame frame) {
		
		//se il frame passato al costruttore è null allora lo sto aprendo per la prima volta (dopo la login), non dopo un logout, lo inizializzo
		if(frame!=null) {
			frame.getContentPane().removeAll();
			frame.getContentPane().revalidate();
			frame.getContentPane().repaint();
			this.frame = frame;

			this.frame.setSize(628,428);
		}
		
		else {
			//caso in cui torni alla schermata principale dopo la logout
			this.frame = new JFrame();
			this.frame.setBounds(100, 100, 628, 428);
		}
		
		this.Client = Client;
		initialize();
		this.frame.setVisible(true);
	}


	private void initialize() {
		
		//frame iniziale 
		frame.setResizable(false);
		frame.setForeground(Color.BLUE);
		frame.getContentPane().setBackground(new Color(255, 250, 205));
		frame.getContentPane().setForeground(new Color(255, 250, 205));
		frame.getContentPane().setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
		//JLabel iniziale WORD QUIZZLE
		JLabel lblWordQuizzle = new JLabel("WORD QUIZZLE");
		lblWordQuizzle.setForeground(new Color(0, 0, 128));
		lblWordQuizzle.setFont(new Font("Sitka Heading", Font.BOLD | Font.ITALIC, 50));
		lblWordQuizzle.setBounds(119, 25, 383, 71);
		frame.getContentPane().add(lblWordQuizzle);
		
		//JLabel per indicare dove inserire lo username
		JLabel lblUsername = new JLabel("username");
		lblUsername.setForeground(new Color(0, 128, 128));
		lblUsername.setFont(new Font("Sitka Subheading", Font.BOLD, 20));
		lblUsername.setBounds(68, 137, 106, 26);
		frame.getContentPane().add(lblUsername);
		
		//JLabel per indicare dove inserire la password
		JLabel lblPassword = new JLabel("password");
		lblPassword.setForeground(new Color(0, 128, 128));
		lblPassword.setFont(new Font("Sitka Subheading", Font.BOLD, 20));
		lblPassword.setBounds(68, 203, 112, 26);
		frame.getContentPane().add(lblPassword);
		
		//textField dove inserire lo username
		textField = new JTextField();
		textField.setBounds(190, 133, 203, 33);
		frame.getContentPane().add(textField);
		textField.setColumns(10);
		
		//textField dove inserire la password
		textField_1 = new JPasswordField();
		textField_1.setColumns(10);
		textField_1.setBounds(190, 199, 203, 33);
		frame.getContentPane().add(textField_1);
		
		//JLabel che fa da indicatore al tasto di registrazione da premere dopo aver inserito le credenziali in caso sia la prima volta che si accede alla piattaforma
		JLabel lblNonSeiAncora = new JLabel("Non sei ancora registrato?");
		lblNonSeiAncora.setForeground(new Color(178, 34, 34));
		lblNonSeiAncora.setFont(new Font("Sitka Display", Font.BOLD, 18));
		lblNonSeiAncora.setBounds(20, 274, 236, 26);
		frame.getContentPane().add(lblNonSeiAncora);
		
		//JLabel che fa da indicatore al tasto di login da premere dopo aver inserito le credenziali in caso non sia la prima volta che si accede alla piattaforma
		JLabel lblSeiGiRegistrato = new JLabel("Sei gi\u00E0 registrato?");
		lblSeiGiRegistrato.setForeground(new Color(178, 34, 34));
		lblSeiGiRegistrato.setFont(new Font("Sitka Display", Font.BOLD, 18));
		lblSeiGiRegistrato.setBounds(368, 274, 236, 26);
		frame.getContentPane().add(lblSeiGiRegistrato);
		
		//JButton da premere per richiedere l'operazione di registrazione 
		JButton btnNewButton = new JButton("REGISTRATI");
		btnNewButton.setForeground(Color.BLUE);
		btnNewButton.setBackground(new Color(224, 255, 255));
		btnNewButton.setFont(new Font("Sitka Display", Font.BOLD, 18));
		btnNewButton.setBounds(30, 310, 187, 33);
		frame.getContentPane().add(btnNewButton);
		
		//JLabel che uso per dare l'esito della registrazione o della login
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setFont(new Font("Sitka Text", Font.BOLD, 16));
		lblNewLabel.setBounds(132, 355, 10000, 26);
		frame.getContentPane().add(lblNewLabel);
		
		//JButton da premere per richiedere l'operazione di login
		JButton btnLogin = new JButton("LOGIN");
		btnLogin.setForeground(new Color(0, 100, 0));
		btnLogin.setFont(new Font("Sitka Display", Font.BOLD, 18));
		btnLogin.setBackground(new Color(224, 255, 255));
		btnLogin.setBounds(354, 310, 187, 33);
		frame.getContentPane().add(btnLogin);
	
		
		//OPERAZIONI CHE VENGONO ESEGUITE IN SEGUITO AL CLICK SUL BOTTONE REGISTRATI----------------------------------
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String result="";
				//prendo username e password inseriti dal cliente nei textfield
				String username = textField.getText(); 
				String pass = new String(textField_1.getPassword()); //getpassword serve a nascondere i caratteri della password
				
				//metto i textfield a vuoto dopo aver fatto un'operazione
				textField.setText("");
				textField_1.setText("");
				//tolgo gli spazi che un utente potrebbe aver inserito accidentalmente
				username = username.trim();
				pass = pass.trim();
				try {
					result = Client.registra_utente(username, pass);
					
					//setto all'etichetta l'esito della registrazione
					lblNewLabel.setText(result);
					if(result.equals("Registrazione effettuata con successo"))
						lblNewLabel.setForeground(Color.GREEN);
						
					else lblNewLabel.setForeground(Color.RED);

				} catch (NotBoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		//OPERAZIONI CHE VENGONO ESEGUITE IN SEGUITO AL CLICK SUL BOTTONE REGISTRATI----------------------------------
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String result="";
				//prendo username e password inseriti dal cliente nei textfield
				String username = textField.getText();
				String pass = new String(textField_1.getPassword());
	
				//metto i textfield a vuoto dopo aver fatto un'operazione
				textField.setText("");
				textField_1.setText("");
				//tolgo gli spazi che un utente potrebbe aver inserito accidentalmente
				username = username.trim();
				pass = pass.trim();
				try {
					result = Client.login(username, pass);
					if(!result.equals("login effettuato con successo .")) {
						//se il login non è andato a buon fine lo notifico all'utente settando la JLabel con l'esito
						lblNewLabel.setText(result);
						lblNewLabel.setForeground(Color.RED);
					}
					else {//se il login è corretto cambio la schermata e vado al menu
						@SuppressWarnings("unused")
						guiLogged guiLogged = new guiLogged(frame, username, Client, false);
						
						}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		
	}
}
