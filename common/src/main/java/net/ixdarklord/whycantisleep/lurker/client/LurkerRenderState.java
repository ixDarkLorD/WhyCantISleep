package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the Lurker's model and renderer need beyond any living entity's. */
public class LurkerRenderState extends LivingEntityRenderState {
    /** How far through its strike it is, 0 to 1. */
    public float attackAnim;
    /** Whether this one is a hallucination: drawn thin and flickering, as something not quite there. */
    public boolean hallucination;
}
