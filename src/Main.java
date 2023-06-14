import java.util.ArrayList;
import java.util.Random;

public class Main {
	
	private static final int ADICIONA = 4000;
	private static final int INATIVO_PROCESSO = 8000;
	private static final int INATIVO_COORDENADOR = 30000;
	private static final int CONSOME_RECURSO_MIN = 5000;
	private static final int CONSOME_RECURSO_MAX = 10000;
	
	private static final Object lock = new Object();
	
	public static void main(String[] args) {		
		criarProcessos(ControladorDeProcessos.getProcessosAtivos());
		inativarCoordenador(ControladorDeProcessos.getProcessosAtivos());
		inativarProcesso(ControladorDeProcessos.getProcessosAtivos());
		acessarRecurso(ControladorDeProcessos.getProcessosAtivos());
	}

	public static void criarProcessos (ArrayList<Processo> processosAtivos) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					synchronized (lock) {
						Processo processo = new Processo(gerarIdUnico(processosAtivos));
						
						if (processosAtivos.isEmpty())
							processo.setEhCoordenador(true);

						processosAtivos.add(processo);
					}
					
					esperar(ADICIONA);
					
				}
			}
		}).start();
	}

	private static int gerarIdUnico(ArrayList<Processo> processosAtivos) {
		Random random = new Random();
		int idRandom = random.nextInt(1000);
		
		for(Processo p : processosAtivos) {
			if(p.getPid() == idRandom)
				return gerarIdUnico(processosAtivos);
		}
		
		return idRandom;
	}

	public static void inativarProcesso(ArrayList<Processo> processosAtivos) {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					esperar(INATIVO_PROCESSO);
					
					synchronized (lock) {
						if (!processosAtivos.isEmpty()) {
							int indexProcessoAleatorio = new Random().nextInt(processosAtivos.size());
							Processo pRemover = processosAtivos.get(indexProcessoAleatorio);
							if (pRemover != null && !pRemover.isCoordenador())
								pRemover.destruir();
						}
					}
				}
			}
		}).start();
	}

	public static void inativarCoordenador(ArrayList<Processo> processosAtivos) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					esperar(INATIVO_COORDENADOR);
					
					synchronized (lock) {
						Processo coordenador = null;
						for (Processo p : processosAtivos) {
							if (p.isCoordenador())
								coordenador = p;
						}
						if (coordenador != null){
							coordenador.destruir();
							System.out.println("Processo coordenador " + coordenador + " destruido.");
						}
					}
				}
			}
		}).start();
	}

	public static void acessarRecurso(ArrayList<Processo> processosAtivos) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Random random = new Random();
				int intervalo = 0;
				while (true) {
					intervalo = random.nextInt(CONSOME_RECURSO_MAX - CONSOME_RECURSO_MIN);
					esperar(CONSOME_RECURSO_MIN + intervalo);
					
					synchronized (lock) {
						if (!processosAtivos.isEmpty()) {
							int indexProcessoAleatorio = new Random().nextInt(processosAtivos.size());
														
							Processo processoConsumidor = processosAtivos.get(indexProcessoAleatorio);
							processoConsumidor.acessarRecursoCompartilhado();
						}
					}
				}
			}
		}).start();
	}
	
	private static void esperar(int segundos) {
		try {
			Thread.sleep(segundos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}