import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;


public class Conexao {
	String logCoordenadorTxt = "logCoordenador.txt";

	private boolean conectado = true;
	public static final String PERMITIR_ACESSO = "PERMITIR";
	public static final String NEGAR_ACESSO = "NAO_PERMITIR";
	private static final int PORTA = 8000;
	private Socket sock;
	private ServerSocket listenSocket;

	public void conectar(Processo coordenador) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// cria um socket TCP para pedidos de conexão
					listenSocket = new ServerSocket(PORTA);

					// fica conectado enquanto o coordenador estiver vivo
					while (conectado) {
						// aguarda ate um cliente pedir por uma conexao
						sock = listenSocket.accept();

						// coloca a resposta em um buffer e envia para o cliente
						DataOutputStream d = new DataOutputStream(sock.getOutputStream());
						String sBuf = "Error!\n";

						if (coordenador.isRecursoEmUso())
							sBuf = NEGAR_ACESSO + "\n";
						else {
							sBuf = PERMITIR_ACESSO + "\n";
						}
						d.write(sBuf.getBytes("UTF-8"));
					}
				} catch (IOException e) {
				}
			}
		}).start();
	}

	public String realizarRequisicao(String mensagem) {
		String rBuf = "ERROR!";
		try {
			// cria um socket TCP para conexao com localhost:PORTA
			Socket sock = new Socket("localhost", PORTA);

			// coloca os dados em um buffer e envia para o servidor
			DataOutputStream d = new DataOutputStream(sock.getOutputStream());
			d.write(mensagem.getBytes("UTF-8"));

			// prepara um buffer para receber a resposta do servidor
			InputStreamReader s = new InputStreamReader(sock.getInputStream());
			BufferedReader rec = new BufferedReader(s);

			// le os dados enviados pela aplicacao servidora
			rBuf = rec.readLine();

			sock.close();
		} catch (Exception e) {
			 }
		return rBuf;
	}

	public void encerrarConexao() {
		conectado = false;
		try {
			sock.close();
		} catch (IOException | NullPointerException e) {
			 e.printStackTrace();
		}
		try {
			listenSocket.close();
		} catch (IOException | NullPointerException e) {
			 e.printStackTrace();
		}
	}
}