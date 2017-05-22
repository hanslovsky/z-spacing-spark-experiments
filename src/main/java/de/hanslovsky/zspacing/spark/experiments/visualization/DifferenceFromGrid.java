package de.hanslovsky.zspacing.spark.experiments.visualization;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.View;
import net.imglib2.type.numeric.RealType;

public class DifferenceFromGrid< T extends RealType< T > > implements RandomAccessible< T >, View
{

	private final RandomAccessible< T > source;

	private final int d;

	private final T t;

	public DifferenceFromGrid( final RandomAccessible< T > source, final int d )
	{
		this.source = source;
		this.d = d;
		this.t = source.randomAccess().get().createVariable();
	}

	@Override
	public DifferenceFromGridAccess< T > randomAccess()
	{
		return new DifferenceFromGridAccess<>( source.randomAccess(), t.createVariable(), d );
	}

	@Override
	public DifferenceFromGridAccess< T > randomAccess( final Interval interval )
	{
		return randomAccess();
	}

	public static class DifferenceFromGridAccess< T extends RealType< T > > extends Point implements RandomAccess< T >
	{

		private final RandomAccess< T > source;

		private final T t;

		private final int d;

		public DifferenceFromGridAccess( final RandomAccess< T > source, final T t, final int d )
		{
			super( source.numDimensions() );
			this.source = source;
			this.t = t;
			this.d = d;
		}

		@Override
		public T get()
		{
			this.source.setPosition( this );
			t.setReal( source.get().getRealDouble() - this.source.getDoublePosition( d ) );
			return t;
		}

		@Override
		public DifferenceFromGridAccess< T > copy()
		{
			return copyRandomAccess();
		}

		@Override
		public DifferenceFromGridAccess< T > copyRandomAccess()
		{
			final DifferenceFromGridAccess< T > other = new DifferenceFromGridAccess<>( source.copyRandomAccess(), t.createVariable(), d );
			other.setPosition( this );
			return other;
		}

	}

	@Override
	public int numDimensions()
	{
		return source.numDimensions();
	}

}
