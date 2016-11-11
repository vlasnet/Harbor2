import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Container {
}

class Ship extends Thread {
	int containersAmount;
	List<Container> hold = new ArrayList<>(containersAmount);
	boolean task; // true = unloading/ship is full, false = loading/ship is
					// empty
	boolean position = false; // true = harbor/wharf, false = road
	Harbor harbor;
	Harbor.Wharf wharf;

	public Ship(int containersAmount, boolean task, Harbor harbor) {
		this.containersAmount = containersAmount;
		this.task = task;
		if (task) {
			for (int i = 0; i < containersAmount; i++) {
				hold.add(new Container());
			}
		}
		this.harbor = harbor;
	}

	public void handlingOperations() {
		try {
			harbor.storageLock.lock();
			if (task) { // ship unloading
				System.out.println("The ship " + Thread.currentThread().getName() + " went to unload");
				if (harbor.getFreeSpace() >= containersAmount) {
					harbor.storage.addAll(hold);
					hold.clear();
					System.out.println("The ship " + Thread.currentThread().getName() + " unloaded " + containersAmount
							+ " containers");
					position = false;
					task = false;
					System.out.println("The ship " + Thread.currentThread().getName() + " is going to the road");
					harbor.haveNoWharf.signal();
				} else {
					System.out.println("The storage in the harbor hasn't enough space");
					position = false;
					System.out.println("The ship " + Thread.currentThread().getName() + " is going to the road");
					try {
						harbor.haveNoWharf.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else { // ship loading
				System.out.println("The ship " + Thread.currentThread().getName() + " went to load");
				if (harbor.hasContainers() >= containersAmount) {
					int counter = 0;
					for (int i = harbor.storage.size() - 1; counter < containersAmount; i--) {
						Container temp = harbor.storage.get(i);
						harbor.storage.remove(i);
						hold.add(temp);
						counter++;
					}
					System.out.println("The ship " + Thread.currentThread().getName() + " loaded " + containersAmount
							+ " containers");
					task = true;
					position = false;
					System.out.println("The ship " + Thread.currentThread().getName() + " is going to the road");
					harbor.haveNoWharf.signal();
				} else {
					System.out.println("The storage in the harbor hasn't enough containers for ship "
							+ Thread.currentThread().getName());
					position = false;
					System.out.println("The ship " + Thread.currentThread().getName() + " is going to the road");
					try {
						harbor.haveNoWharf.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			harbor.storageLock.unlock();
		}
	}

	public void run() {
		while (true) {
			harbor.entranceShip();
			System.out.println("The ship " + Thread.currentThread().getName() + " is going to get the wharf...");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			position = true;
			System.out.println("The ship " + Thread.currentThread().getName() + " arrived to the harbor");
			handlingOperations();
		}
	}
}

class Harbor {
	int storageCapacity;
	List<Container> storage = new ArrayList<>(storageCapacity);
	Phaser ships = new Phaser(4);
	Phaser wharfs = new Phaser(4);
	ReentrantLock storageLock = new ReentrantLock(true);
	final Condition haveNoWharf = storageLock.newCondition();

	class Wharf extends Thread {
		boolean state = true; // true = free; false = busy

		public void run() {
			while (true) {
				wharfs.arrive();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
				ships.arriveAndAwaitAdvance();
			}
		}
	}

	public Harbor(int storageCapacity) {
		this.storageCapacity = storageCapacity;
		Wharf wharf1 = new Wharf();
		wharf1.start();
		Wharf wharf2 = new Wharf();
		wharf2.start();
		Wharf wharf3 = new Wharf();
		wharf3.start();
		Wharf wharf4 = new Wharf();
		wharf4.start();
	}

	public void entranceShip() {
		ships.arriveAndAwaitAdvance();
		wharfs.arrive();
	}

	public int getFreeSpace() {
		try {
			storageLock.lock();
			return storageCapacity - storage.size();
		} finally {
			storageLock.unlock();
		}
	}

	public int hasContainers() {
		try {
			storageLock.lock();
			return storage.size();
		} finally {
			storageLock.unlock();
		}
	}
}

public class MainHarbor2 {

	public static void main(String[] args) {
		Harbor harbor = new Harbor(100);

		Ship ship1 = new Ship(20, true, harbor);
		ship1.start();

		Ship ship2 = new Ship(15, false, harbor);
		ship2.start();

		Ship ship3 = new Ship(17, false, harbor);
		ship3.start();

		Ship ship4 = new Ship(45, true, harbor);
		ship4.start();

		Ship ship5 = new Ship(10, true, harbor);
		ship5.start();

		Ship ship6 = new Ship(5, false, harbor);
		ship6.start();
	}

}
