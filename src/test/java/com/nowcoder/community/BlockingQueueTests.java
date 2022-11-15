package com.nowcoder.community;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTests {
    //main中测试阻塞队列
    public static void main(String[] args) {
        //实例化阻塞队列,队列大小为10(多态:接口的实现类)
        BlockingQueue queue = new ArrayBlockingQueue(10);
            //生产者生产的速度快(20ms生产一个数据)
        new Thread(new Producer(queue)).start();//实例化一个生产者线程,用于生产数据(最多生产100个)
            //消费者消费的速度慢(0-1000ms内随机消费一个数据)
        new Thread(new Consumer(queue)).start();//实例化三个消费者线程,用于消费数据
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
    }

}
//生产者线程(因此需要实现Runnable接口)
class Producer implements Runnable {

    //当实例化生产者线程时,需要传入阻塞队列
    private BlockingQueue<Integer> queue;//此时阻塞队列中存储整数
    //构造器中传入阻塞队列
    public Producer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }
    @Override
    public void run() {
        try {
            //run()中实现: 生产者不断生产数据,放入阻塞队列中
            for (int i = 0; i < 100; i++) { //生产者最多产出100个数据
                Thread.sleep(20);//间隔20ms
                queue.put(i);//每次都生产一个数,存放到阻塞队列中
                System.out.println(Thread.currentThread().getName() + "生产:" + queue.size());//当前生产者线程  生产:  此时阻塞队列的大小
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
//消费者线程
class Consumer implements Runnable {
    //当实例化消费线程时,需要传入阻塞队列
    private BlockingQueue<Integer> queue;
    //构造器中传入阻塞队列
    public Consumer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            //run()中实现: 消费者不断消耗数据,从阻塞队列中获取
            while (true) {//只要队列有数据,则一直进行消费
                Thread.sleep(new Random().nextInt(1000));//消费者(用户)的时间间隔:随机的时间(0-1000ms之间)
                queue.take();//从阻塞队列中获取数据
                System.out.println(Thread.currentThread().getName() + "消费:" + queue.size());//当前消费者线程  消费:  此时阻塞队列大小
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}