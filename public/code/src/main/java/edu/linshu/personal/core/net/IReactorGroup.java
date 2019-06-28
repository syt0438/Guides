package edu.linshu.personal.core.net;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/28 9:38
 */
public interface IReactorGroup {
    int reactorCount();

    IReactor get(int index);

    void start();

    void close();

    IReactor election();

    void setSubReactorGroup(ReactorGroup subReactorGroup);

    IReactorGroup getSubReactorGroup();
}
