package edu.linshu.personal.core.net.jdk.nio.reactor;

import edu.linshu.personal.core.utils.RandomUtils;

import java.util.Objects;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 10:26
 */
public abstract class ReactorGroup implements IReactorGroup {

    @Override
    public void start() {
        registerShutdownHook();

        for (int i = 0; i < reactorCount(); i++) {
            IReactor reactor = get(i);

            reactor.start();
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < reactorCount(); i++) {
                IReactor reactor = get(i);

                reactor.close();

                IReactorGroup subReactorGroup = getSubReactorGroup();
                if (Objects.nonNull(subReactorGroup)) {
                    subReactorGroup.close();
                }
            }

            System.out.println("关闭钩子执行完成");
        }));
    }

    @Override
    public void close() {
        for (int i = 0; i < reactorCount(); i++) {
            IReactor reactor = get(i);

            reactor.close();
        }
    }

    @Override
    public IReactor election() {
        int index = RandomUtils.random(reactorCount());

        return get(index);
    }

    @Override
    public void setSubReactorGroup(ReactorGroup subReactorGroup) {
    }

    @Override
    public IReactorGroup getSubReactorGroup() {
        return null;
    }
}
