package io.apptik.rhub.example;


import java.util.concurrent.TimeUnit;

import io.apptik.rhub.DefaultRxJava1Hub;
import io.apptik.rhub.RxJava1Hub;
import io.apptik.rhub.RxJava1ProxyType;
import rx.Observable;

public class RxJava1 {

    public static void main(String[] args) {
        RxJava1Hub rxJava1Hub = new DefaultRxJava1Hub() {
            @Override
            public RxJava1ProxyType getProxyType(Object tag) {
                return RxJava1ProxyType.BehaviorRelayProxy;
            }
        };
        // generalExample(rxJava1Hub);
        shieldExample(rxJava1Hub);
    }


    private static void generalExample(RxJava1Hub rxJava1Hub) {
        Observable src1 = Observable.from(new Integer[]{1, 3, 5, 7, 11, 13});
        Observable src2 = Observable.interval(1, TimeUnit.SECONDS);
        rxJava1Hub.addUpstream("src1", src1);

        rxJava1Hub.getPub("src1").subscribe(o -> {
            System.out.println("consumer1 (src1) got: " + o);
        });

        rxJava1Hub.getPub("src1").subscribe(System.out::println);
        rxJava1Hub.getPub("src1.1").subscribe(o -> {
            System.out.println("consumer1 (src1.1) got: " + o);
        });

        rxJava1Hub.addUpstream("src1.1", src1.repeat(1));
        rxJava1Hub.addUpstream("src2", src2.onBackpressureBuffer());

        rxJava1Hub.getPub("src1").subscribe(o -> {
            System.out.println("consumer2 (src1) got: " + o);
        });

        rxJava1Hub.getPub("src1.1").subscribe(o -> {
            System.out.println("consumer2 (src1.1) got: " + o);
        });
        rxJava1Hub.getPub("src2").subscribe(o -> {
            System.out.println("consumer2 (src2) got: " + o);
        });
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                rxJava1Hub.addUpstream("src1.1", Observable.interval(1, TimeUnit.SECONDS));
                Thread.sleep(5000);
                rxJava1Hub.clearUpstream();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * this example demonstrates the Concept os a "ShieldObs". ShieldObs could be seen and a
     * abstraction layer or contract for the consumers. Its purpose is to filter, secure and/or
     * adapt the data coming from the RxJava1Hub and to relieve the consumer from this tasks.
     * <p>
     * It can compared to an Arduino shield :)
     */
    private static void shieldExample(RxJava1Hub rxJava1Hub) {

        Shield shield = new Shield(rxJava1Hub);
        Observable srcLong = Observable.from(
                new Long[]{1l, 3l, 5l, 7l, 11l, 13l, 101l, 201l, 301l, 401l, 501l});
        Observable srcString = Observable.from(new String[]{"a", "b", "c", "d", "f"});

        shield.getNames().subscribe(o -> {
            System.out.println(String.format("consumer1(%s) got:%s",
                    Thread.currentThread().getName(), o));
        });

        shield.getBigOnes()
                .subscribe(o -> {
                    if (o.equals(301l)) {
                        rxJava1Hub.removeUpstream("topic1", srcLong);
                    }
                    System.out.println(String.format("consumer2(%s) got:%s",
                            Thread.currentThread().getName(), o));

                });


        rxJava1Hub.addUpstream("topic1", srcLong);
        rxJava1Hub.addUpstream("topic1", srcString);


        //wait a little
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class Shield {
        final RxJava1Hub rxJava1Hub;

        Shield(RxJava1Hub rxJava1Hub) {
            this.rxJava1Hub = rxJava1Hub;
        }

        Observable<String> getNames() {
            return rxJava1Hub.getPub("topic1", String.class);
        }

        Observable<Long> getBigOnes() {
            return rxJava1Hub.getPub("topic1", Long.class)
                    .filter(o -> o > 100);
        }
    }
}
