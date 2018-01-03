package com.n26.task.streamstat.model;

public class Statistics {
    private final double sum;
    private final double avg;
    private final double max;
    private final double min;
    private final long count;

    private Statistics(double sum,
                       double avg,
                       double max,
                       double min,
                       long count) {
        this.sum = sum;
        this.avg = avg;
        this.max = max;
        this.min = min;
        this.count = count;
    }

    public double getSum() {
        return sum;
    }

    public double getAvg() {
        return avg;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Statistics that = (Statistics) o;

        return doubleEqualsWithAccuracy(that.sum, sum) &&
                doubleEqualsWithAccuracy(that.avg, avg) &&
                Double.compare(that.max, max) == 0 &&
                Double.compare(that.min, min) == 0 &&
                count == that.count;
    }

    private static boolean doubleEqualsWithAccuracy(double d1, double d2) {
        return Math.abs(d1 - d2) < 0.0000001d;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(sum);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(avg);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(max);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(min);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "sum=" + sum +
                ", avg=" + avg +
                ", max=" + max +
                ", min=" + min +
                ", count=" + count +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double sum;
        private double avg;
        private double max;
        private double min;
        private long count;

        public Builder withSum(double sum) {
            this.sum = sum;
            return this;
        }

        public Builder withAvg(double avg) {
            this.avg = avg;
            return this;
        }

        public Builder withMax(double max) {
            this.max = max;
            return this;
        }

        public Builder withMin(double min) {
            this.min = min;
            return this;
        }

        public Builder withCount(long count) {
            this.count = count;
            return this;
        }

        public Statistics build() {
            return new Statistics(sum, avg, max, min, count);
        }
    }
}
