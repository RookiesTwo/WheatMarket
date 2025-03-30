package top.rookiestwo.wheatmarket.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class LaptopBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING= BlockStateProperties.FACING;
    public static final MapCodec<LaptopBlock> CODEC = BlockBehaviour.simpleCodec(LaptopBlock::new);

    public LaptopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        Direction dir = state.get(FACING);
        return switch (dir) {
            case NORTH -> Block.box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f);
            case SOUTH -> Block.box(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f);//看似没问题，但是我不确定这个碰撞体积是不是适配笔记本电脑的
            case EAST -> Block.box(0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
            case WEST -> Block.box(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f);
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).with(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
