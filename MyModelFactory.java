package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	/**
	 * build
	 * @param setup the game setup
	 * @param mrX MrX player
	 * @param detectives detective players
	 * @return
	 */
	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		return new MyModel(setup,mrX,detectives);
	}

	/**
	 *  MyModel is an implementation of interface Model.
	 */
	private class MyModel implements Model {
		private Board.GameState gameState;
		private Set<Observer> observers;
		private final GameSetup setup;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private Board currentBoard;

		public MyModel (GameSetup setup,
						Player mrX,
						ImmutableList<Player> detectives){
			this.setup = setup;
			this.mrX = mrX;
			this.detectives = detectives;
			this.observers = new HashSet<Observer>();
			this.gameState = new MyGameStateFactory().build(setup,mrX,detectives);
			this.currentBoard = getCurrentBoard();
		}

		@Nonnull @Override public Board getCurrentBoard() {
			return gameState;
		}
		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) throw  new NullPointerException("Null Observer to register");
			if (observers.contains(observer)) throw new IllegalArgumentException("Same Observer Twice!");
			observers.add(observer);
		}
		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException("Null Observer to unregister!");
			if (observers.isEmpty()) throw new IllegalArgumentException("Empty Observer to unregister!");
			if (!observers.contains(observer)) throw  new IllegalArgumentException("No observer to unregister");
			observers.remove(observer);

		}
		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}
		@Override
		public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			Observer.Event event = gameState.getWinner().isEmpty()						//是否有赢家
					? Observer.Event.MOVE_MADE: Observer.Event.GAME_OVER;

			for (Observer o:observers) {												//通知所有Observers
				o.onModelChanged(gameState,event);
			}
		}
	}
}
