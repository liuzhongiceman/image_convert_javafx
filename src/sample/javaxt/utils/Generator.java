package javaxt.utils;
import java.util.Iterator;
import java.util.NoSuchElementException;

//******************************************************************************
//**  Generator
//******************************************************************************
/**
 *   A custom iterator that yields its values one at a time. This is a great
 *   alternative to arrays or lists when dealing with large data. By yielding
 *   one entry at a time, this iterator can help avoid out of memory exceptions.
 *
 *   Subclasses must define a method called {@link #run()} and may call
 *   {@link yield(T)} to return values one at a time. Example:
 <pre>
    Generator&lt;String&gt; generator = new Generator&lt;String&gt;() {

        @Override
        public void run() {

            BufferedReader br = file.getBufferedReader("UTF-8");

            String row;
            while ((row = br.readLine()) != null){  
                yield(row);
            }

            br.close();
        }
    };
 </pre>
 * 
 *   Clients can iterate through the generated results using standard iterators
 *   or an enhanced for loop like this:
 <pre>
    for (String row : generator){
        System.out.println(row);
    }
 </pre>
 *
 *   @author Michael Herrmann
 *   https://github.com/mherrmann/java-generator-functions
 *
 ******************************************************************************/

public abstract class Generator<T> implements Iterable<T> {

	private class Condition {
		private boolean isSet;
		public synchronized void set() {
			isSet = true;
			notify();
		}
		public synchronized void await() throws InterruptedException {
			try {
				if (isSet)
					return;
				wait();
			} finally {
				isSet = false;
			}
		}
	}

	static ThreadGroup THREAD_GROUP;

	Thread producer;
	private boolean hasFinished;
	private final Condition itemAvailableOrHasFinished = new Condition();
	private final Condition itemRequested = new Condition();
	private T nextItem;
	private boolean nextItemAvailable;
	private RuntimeException exceptionRaisedByProducer;

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return waitForNext();
			}
			@Override
			public T next() {
				if (!waitForNext())
					throw new NoSuchElementException();
				nextItemAvailable = false;
				return nextItem;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			private boolean waitForNext() {
				if (nextItemAvailable)
					return true;
				if (hasFinished)
					return false;
				if (producer == null)
					startProducer();
				itemRequested.set();
				try {
					itemAvailableOrHasFinished.await();
				} catch (InterruptedException e) {
					hasFinished = true;
				}
				if (exceptionRaisedByProducer != null)
					throw exceptionRaisedByProducer;
				return !hasFinished;
			}
		};
	}

	protected abstract void run() throws InterruptedException;

	protected void yield(T element) throws InterruptedException {
		nextItem = element;
		nextItemAvailable = true;
		itemAvailableOrHasFinished.set();
		itemRequested.await();
	}

	private void startProducer() {
		assert producer == null;
		if (THREAD_GROUP == null)
			THREAD_GROUP = new ThreadGroup("generatorfunctions");
		producer = new Thread(THREAD_GROUP, new Runnable() {
			@Override
			public void run() {
				try {
					itemRequested.await();
					Generator.this.run();
				} catch (InterruptedException e) {
					// No need to do anything here; Remaining steps in run()
					// will cleanly shut down the thread.
				} catch (RuntimeException e) {
					exceptionRaisedByProducer = e;
				}
				hasFinished = true;
				itemAvailableOrHasFinished.set();
			}
		});
		producer.setDaemon(true);
		producer.start();
	}

	@Override
	protected void finalize() throws Throwable {
		producer.interrupt();
		producer.join();
		super.finalize();
	}
}